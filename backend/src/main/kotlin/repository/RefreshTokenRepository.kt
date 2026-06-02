@file:OptIn(ExperimentalUuidApi::class)

package com.ghkasra.discordclone.repository

import com.ghkasra.discordclone.repository.RefreshTokens.accessedAt
import com.ghkasra.discordclone.repository.RefreshTokens.createdAt
import com.ghkasra.discordclone.repository.RefreshTokens.deviceId
import com.ghkasra.discordclone.repository.RefreshTokens.expiresAt
import com.ghkasra.discordclone.repository.RefreshTokens.id
import com.ghkasra.discordclone.repository.RefreshTokens.isRevoked
import com.ghkasra.discordclone.repository.RefreshTokens.tokenHash
import com.ghkasra.discordclone.repository.RefreshTokens.updatedAt
import com.ghkasra.discordclone.repository.RefreshTokens.userId
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.jetbrains.exposed.v1.jdbc.upsertReturning

@JvmInline value class RefreshTokenHash(val value: String)

@JvmInline value class RefreshTokenId(val value: Uuid)

@Serializable @JvmInline value class DeviceId(val value: String)

data class RefreshTokenCredentials(
    val id: RefreshTokenId,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant,
    val accessedAt: Instant,
    val isRevoked: Boolean,
    val isExpired: Boolean,
    val userId: UserId,
    val deviceId: DeviceId,
)

object RefreshTokens : UuidTable("refresh_tokens", uuidVersion = UuidVersion.V7) {
  val tokenHash = text("token_hash").uniqueIndex()
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
  val accessedAt = timestamp("accessed_at").clientDefault { Clock.System.now() }
  val expiresAt = timestamp("expires_at")
  val isRevoked = bool("is_revoked").clientDefault { false }
  val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
  val deviceId = text("device_id")

  init {
    uniqueIndex(userId, deviceId)
  }
}

class RefreshTokenRepository(val db: Database) {
  init {
    transaction(db) { SchemaUtils.create(RefreshTokens) }
  }

  fun save(
      tokenHash: RefreshTokenHash,
      userId: UserId,
      deviceId: DeviceId,
      expiresIn: Duration,
  ): RefreshTokenCredentials =
      transaction(db) {
        RefreshTokens.upsertReturning(RefreshTokens.userId, RefreshTokens.deviceId) {
              it[this.userId] = userId.value
              it[this.tokenHash] = tokenHash.value
              it[this.deviceId] = deviceId.value
              it[expiresAt] = Clock.System.now().plus(expiresIn)
            }
            .single()
            .toRefreshTokenCredentials()
      }

  fun findValidTokenByHash(token: RefreshTokenHash, deviceId: DeviceId): RefreshTokenCredentials? =
      transaction(db) {
        RefreshTokens.updateReturning(
                where = {
                  tokenHash.eq(token.value) and
                      isRevoked.eq(false) and
                      expiresAt.greater(Clock.System.now()) and
                      RefreshTokens.deviceId.eq(deviceId.value)
                }
            ) {
              it[this.accessedAt] = Clock.System.now()
              it[this.updatedAt] = Clock.System.now()
            }
            .singleOrNull()
            ?.toRefreshTokenCredentials()
      }

  fun revokeAll(userId: UserId) =
      transaction(db) {
        RefreshTokens.update(where = { RefreshTokens.userId.eq(userId.value) }) {
          it[isRevoked] = true
          it[updatedAt] = Clock.System.now()
        }
      }

  fun revoke(userId: UserId, deviceId: DeviceId) =
      transaction(db) {
        RefreshTokens.update({
          RefreshTokens.userId.eq(userId.value) and RefreshTokens.deviceId.eq(deviceId.value)
        }) {
          it[isRevoked] = true
          it[updatedAt] = Clock.System.now()
        }
      }

  fun listByUserId(userId: UserId): List<RefreshTokenCredentials> =
      transaction(db) {
        RefreshTokens.selectAll()
            .where { RefreshTokens.userId.eq(userId.value) }
            .map { it.toRefreshTokenCredentials() }
      }
}

fun ResultRow.toRefreshTokenCredentials() =
    RefreshTokenCredentials(
        id = RefreshTokenId(get(id).value),
        createdAt = get(createdAt),
        updatedAt = get(updatedAt),
        expiresAt = get(expiresAt),
        isRevoked = get(isRevoked),
        userId = UserId(get(userId).value),
        isExpired = get(expiresAt) < Clock.System.now(),
        deviceId = DeviceId(get(deviceId)),
        accessedAt = get(accessedAt),
    )
