package com.ghkasra.discordclone.repository

import com.ghkasra.discordclone.service.Username
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.updateReturning

@Serializable
data class User(
    val id: UserId,
    val username: Username,
    val createdAt: Instant,
    val updatedAt: Instant,
    val email: Email,
)

object Users : UuidTable("users", uuidVersion = UuidVersion.V7) {
  val username = text("username").uniqueIndex()
  val email = text("email").uniqueIndex()
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

class UserRepository(val db: Database) {
  init {
    transaction(db) { SchemaUtils.create(Users) }
  }

  fun update(
      id: UserId,
      username: Username?,
      email: Email?,
  ) =
      transaction(db) {
        Users.updateReturning(where = { Users.id eq id.value }) {
              username?.let { username -> it[Users.username] = username.value }
              email?.let { email -> it[Users.email] = email.value }
            }
            .single()
            .toUser()
      }

  fun get(id: UserId): User =
      transaction(db) { Users.selectAll().where { Users.id eq id.value }.single().toUser() }

  fun findByUsername(username: Username): User? =
      transaction(db) {
        Users.selectAll().where { Users.username eq username.value }.singleOrNull()?.toUser()
      }

  fun create(
      username: Username,
      email: Email,
  ): User =
      transaction(db) {
        Users.insertReturning {
              it[this.username] = username.value
              it[this.email] = email.value
            }
            .single()
            .toUser()
      }
}

fun ResultRow.toUser() =
    User(
        id = UserId(get(Users.id).value),
        username = Username(get(Users.username)),
        email = Email(get(Users.email)),
        createdAt = get(Users.createdAt),
        updatedAt = get(Users.updatedAt),
    )

@Serializable @JvmInline value class Email(val value: String)

@Serializable @JvmInline value class UserId(val value: Uuid)
