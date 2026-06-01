@file:OptIn(ExperimentalUuidApi::class)

package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ChannelId
import com.ghkasra.discordclone.model.Message
import com.ghkasra.discordclone.model.MessageId
import com.ghkasra.discordclone.model.SaveMessageRequest
import kotlin.let
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class MessageRepository(val db: Database) {

  init {
    transaction(db) { SchemaUtils.create(Messages) }
  }

  fun listMessages(channel: ChannelId, count: Int): List<Message> =
      transaction(db) {
        (Messages innerJoin Users)
            .selectAll()
            .where { Messages.channelId.eq(channel.value) }
            .orderBy(Messages.id, SortOrder.DESC)
            .limit(count)
            .map { it.toMessage() }
            .toList()
      }

  fun save(request: SaveMessageRequest): Message =
      transaction(db) {
        val insertedMessage =
            Messages.insert {
                  it[content] = request.content
                  it[authorId] = request.authorId.value
                  it[channelId] = request.channelId.value
                }
                .resultedValues
                ?.single()
                .let { checkNotNull(it) { "Insert statement returned nothing" } }

        (Messages innerJoin Users)
            .selectAll()
            .where { Messages.id.eq(insertedMessage[Messages.id].value) }
            .single()
            .toMessage()
      }
}

fun ResultRow.toMessage() =
    Message(
        id = MessageId(get(Messages.id).value),
        createdAt = get(Messages.createdAt),
        updatedAt = get(Messages.updatedAt),
        content = get(Messages.content),
        author = toUser(),
        channelId = ChannelId(get(Messages.channelId)),
    )

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

@Serializable @JvmInline value class PasswordHash(val value: String)

@Serializable
data class CreateUserRequest(val username: Username, val password: PasswordHash, val email: Email)

data class UpdateUserRequest(
    val id: UserId,
    val username: Username?,
    val password: PasswordHash?,
    val email: Email?,
)

data class GetUserRequest(val id: UserId)

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
  val passwordHash = text("password_hash")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

object Messages : UuidTable("messages", uuidVersion = UuidVersion.V7) {
  val channelId = text("channel_id")
  val content = text("content")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
  val authorId = reference("author_id", Users.id, ReferenceOption.RESTRICT)
}

class UserRepository(val db: Database) {
  init {
    transaction(db) { SchemaUtils.create(Users) }
  }

  fun update(request: UpdateUserRequest) =
      transaction(db) {
        Users.update({ Users.id eq request.id.value }) {
          request.username?.let { username -> it[Users.username] = username.value }
          request.email?.let { email -> it[Users.email] = email.value }
          request.password?.let { pass -> it[Users.passwordHash] = pass.value }
        }
      }

  fun get(request: GetUserRequest): User =
      transaction(db) { Users.selectAll().where { Users.id eq request.id.value }.single().toUser() }

  fun save(request: CreateUserRequest): User =
      transaction(db) {
        Users.insert {
              it[username] = request.username.value
              it[email] = request.email.value
              it[passwordHash] = request.password.value
            }
            .resultedValues
            ?.single()
            .let { checkNotNull(it) { "Insert statement returned nothing" } }
            .toUser()
      }
}
