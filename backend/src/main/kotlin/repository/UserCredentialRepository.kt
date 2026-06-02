package com.ghkasra.discordclone.repository

import com.ghkasra.discordclone.service.Username
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class UserCredential(
    val username: Username,
    val passwordHash: PasswordHash,
    val createdAt: Instant,
    val updatedAt: Instant,
)

object UserCredentials : UuidTable("user_credentials", uuidVersion = UuidVersion.V7) {
  val username = reference("username", Users.username, ReferenceOption.CASCADE)
  val passwordHash = text("password_hash")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

class UserCredentialRepository(val db: Database) {
  init {
    transaction(db) { SchemaUtils.create(UserCredentials) }
  }

  fun findByUsername(username: Username): UserCredential? =
      transaction(db) {
        UserCredentials.selectAll()
            .where { UserCredentials.username.eq(username.value) }
            .singleOrNull()
            ?.toUserCredential()
      }

  fun create(username: Username, password: PasswordHash): UserCredential =
      transaction(db) {
        UserCredentials.insertReturning {
              it[this.username] = username.value
              it[passwordHash] = password.value
            }
            .single()
            .toUserCredential()
      }
}

fun ResultRow.toUserCredential() =
    UserCredential(
        username = Username(get(UserCredentials.username)),
        passwordHash = PasswordHash(get(UserCredentials.passwordHash)),
        createdAt = get(UserCredentials.createdAt),
        updatedAt = get(UserCredentials.updatedAt),
    )

@Serializable @JvmInline value class PasswordHash(val value: String)
