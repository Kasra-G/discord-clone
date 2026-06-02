@file:OptIn(ExperimentalUuidApi::class)

package com.ghkasra.discordclone.repository

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
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class Message(
    val channelId: ChannelId,
    val content: String,
    val author: User,
    val id: MessageId,
    val createdAt: Instant,
    val updatedAt: Instant,
)

object Messages : UuidTable("messages", uuidVersion = UuidVersion.V7) {
  val channelId = text("channel_id")
  val content = text("content")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
  val authorId = reference("author_id", Users.id, ReferenceOption.RESTRICT)
}

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

  fun save(channelId: ChannelId, content: String, authorId: UserId): Message =
      transaction(db) {
        val insertedMessageId = Messages.insertAndGetId {
          it[this.content] = content
          it[this.authorId] = authorId.value
          it[this.channelId] = channelId.value
        }

        (Messages innerJoin Users)
            .selectAll()
            .where { Messages.id.eq(insertedMessageId.value) }
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

@Serializable
@JvmInline
value class ChannelId(val value: String) {
  companion object {
    val DEFAULT = ChannelId("default")
  }
}

@Serializable @JvmInline value class MessageId(val value: Uuid)
