@file:OptIn(ExperimentalUuidApi::class)

package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ChannelId
import com.ghkasra.discordclone.model.Message
import com.ghkasra.discordclone.model.MessageId
import com.ghkasra.discordclone.model.SaveMessageRequest
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MessageRepository(val db: Database) {

  init {
    transaction(db) { SchemaUtils.create(Messages) }
  }

  private object Messages : Table("messages") {
    val id = uuid("id")
    val channelId = text("channel_id")
    val content = text("content")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val sentBy = text("sent_by")

    init {
      uniqueIndex(channelId, id)
    }
  }

  private fun ResultRow.toMessage() =
      Message(
          id = MessageId(this[Messages.id]),
          createdAt = this[Messages.createdAt],
          updatedAt = this[Messages.updatedAt],
          content = this[Messages.content],
          sentBy = Username(this[Messages.sentBy]),
          channelId = ChannelId(this[Messages.channelId]),
      )

  fun listMessages(channel: ChannelId, count: Int): List<Message> =
      transaction(db) {
        Messages.selectAll()
            .where { Messages.channelId.eq(channel.value) }
            .orderBy(Messages.id, SortOrder.DESC)
            .limit(count)
            .map { it.toMessage() }
            .toList()
      }

  fun save(request: SaveMessageRequest): Message =
      transaction(db) {
        Messages.insert {
              it[id] = Uuid.generateV7()
              it[createdAt] = Clock.System.now()
              it[updatedAt] = Clock.System.now()
              it[content] = request.content
              it[sentBy] = request.sentBy.value
              it[channelId] = request.channelId.value
            }
            .resultedValues
            ?.single()
            .let { checkNotNull(it) { "Insert statement returned nothing" } }
            .toMessage()
      }
}
