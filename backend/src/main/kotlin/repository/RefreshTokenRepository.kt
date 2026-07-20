package com.ghkasra.discordclone.repository

import com.ghkasra.discordclone.repository.RefreshTokens.createdAt
import com.ghkasra.discordclone.repository.RefreshTokens.deviceId
import com.ghkasra.discordclone.repository.RefreshTokens.expiresAt
import com.ghkasra.discordclone.repository.RefreshTokens.id
import com.ghkasra.discordclone.repository.RefreshTokens.isRevoked
import com.ghkasra.discordclone.repository.RefreshTokens.isUsed
import com.ghkasra.discordclone.repository.RefreshTokens.isValidToken
import com.ghkasra.discordclone.repository.RefreshTokens.tokenHash
import com.ghkasra.discordclone.repository.RefreshTokens.updatedAt
import com.ghkasra.discordclone.repository.RefreshTokens.usedAt
import com.ghkasra.discordclone.repository.RefreshTokens.userId
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning

@JvmInline value class RefreshTokenHash(val value: String)

@JvmInline value class RefreshTokenId(val value: Uuid)

@Serializable @JvmInline value class DeviceId(val value: String)

data class RefreshTokenCredentials(
    val id: RefreshTokenId,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant,
    val isRevoked: Boolean,
    val isUsed: Boolean,
    val userId: UserId,
    val deviceId: DeviceId,
)

object RefreshTokens : UuidTable("refresh_tokens", uuidVersion = UuidVersion.V7) {
  val tokenHash = text("token_hash").uniqueIndex()
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
  val usedAt = timestamp("used_at").clientDefault { Clock.System.now() }
  val expiresAt = timestamp("expires_at")
  val isRevoked = bool("is_revoked").clientDefault { false }
  val isUsed = bool("is_used").clientDefault { false }
  val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
  val deviceId = text("device_id")

  val isValidToken: Op<Boolean>
    get() = not(isUsed) and not(isRevoked) and (expiresAt greater Clock.System.now())

  init {
    index(
        customIndexName = "idx_user_device_valid_partial",
        isUnique = false,
        filterCondition = { not(isUsed) and not(isRevoked) },
        columns = arrayOf(userId, deviceId),
    )
  }
}

class TokenNotFoundException(hash: RefreshTokenHash) :
    NoSuchElementException("Token hash $hash not found in database") {
  override fun fillInStackTrace(): Throwable =
      this // Disables expensive stack traces for performance
}

class RefreshTokenRepository(val db: Database) {
  init {
    transaction(db) {
      SchemaUtils.create(RefreshTokens)
    }
  }

  @WithSpan
  suspend fun save(
      tokenHash: RefreshTokenHash,
      userId: UserId,
      deviceId: DeviceId,
      expiresIn: Duration,
  ): RefreshTokenCredentials =
      withContext(Context.current().asContextElement()) {
        suspendTransaction(db) {
          RefreshTokens.insertReturning {
                it[this.userId] = userId.value
                it[this.tokenHash] = tokenHash.value
                it[this.deviceId] = deviceId.value
                it[expiresAt] = Clock.System.now().plus(expiresIn)
              }
              .single()
              .toRefreshTokenCredentials()
        }
      }

  @WithSpan
  suspend fun findValidTokenByHash(token: RefreshTokenHash): Result<RefreshTokenCredentials> =
      runCatching {
        withContext(Context.current().asContextElement()) {
          suspendTransaction(db) {
            RefreshTokens.updateReturning(where = { (tokenHash eq token.value) and isValidToken }) {
                  it[this.updatedAt] = Clock.System.now()
                }
                .singleOrNull()
                ?.toRefreshTokenCredentials() ?: throw TokenNotFoundException(token)
          }
        }
      }

  @WithSpan
  suspend fun findAnyTokenByHash(token: RefreshTokenHash): Result<RefreshTokenCredentials> =
      runCatching {
        withContext(Context.current().asContextElement()) {
          suspendTransaction(db) {
            RefreshTokens.updateReturning(where = { tokenHash eq token.value }) {
                  it[this.updatedAt] = Clock.System.now()
                }
                .singleOrNull()
                ?.toRefreshTokenCredentials() ?: throw TokenNotFoundException(token)
          }
        }
      }

  @WithSpan
  fun revokeAll(userId: UserId) =
      transaction(db) {
        RefreshTokens.update(where = { RefreshTokens.userId.eq(userId.value) }) {
          it[isRevoked] = true
          it[updatedAt] = Clock.System.now()
        }
      }

  @WithSpan
  suspend fun revokeExistingDeviceTokens(userId: UserId, deviceId: DeviceId) =
      withContext(Context.current().asContextElement()) {
        suspendTransaction(db) {
          RefreshTokens.update({
            RefreshTokens.userId.eq(userId.value) and RefreshTokens.deviceId.eq(deviceId.value)
          }) {
            it[isRevoked] = true
            it[updatedAt] = Clock.System.now()
          }
        }
      }

  @WithSpan
  fun listByUserId(userId: UserId): List<RefreshTokenCredentials> =
      transaction(db) {
        RefreshTokens.selectAll()
            .where { RefreshTokens.userId.eq(userId.value) and isValidToken }
            .map { it.toRefreshTokenCredentials() }
      }

  @WithSpan
  suspend fun markAsUsed(tokenHash: RefreshTokenHash) {
    withContext(Context.current().asContextElement()) {
      suspendTransaction(db) {
        RefreshTokens.update({
          (RefreshTokens.tokenHash eq tokenHash.value) and isValidToken
        }) {
          it[isUsed] = true
          it[usedAt] = Clock.System.now()
          it[updatedAt] = Clock.System.now()
        }
      }
    }
  }
}

fun ResultRow.toRefreshTokenCredentials() =
    RefreshTokenCredentials(
        id = RefreshTokenId(get(id).value),
        createdAt = get(createdAt),
        updatedAt = get(updatedAt),
        expiresAt = get(expiresAt),
        isRevoked = get(isRevoked),
        isUsed = get(isUsed),
        userId = UserId(get(userId).value),
        deviceId = DeviceId(get(deviceId)),
        usedAt = get(usedAt),
    )
