package com.ghkasra.discordclone

import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@OptIn(ExperimentalUuidApi::class)
fun Application.configureExposed() {
  Database.connect("jdbc:sqlite:chat.db")
  transaction {
    SchemaUtils.create(Messages)
    val id =
        Messages.insert {
          it[messageId] = Uuid.generateV7()
          it[content] = "my message!"
          it[timestamp] = Clock.System.now()
          it[sentBy] = Username.SYSTEM.value
        } get Messages.messageId
    log.info("Saved message to database: $id")

    val message = Messages.selectAll().where { Messages.messageId.eq(id) }.single()
    val uuid = message[Messages.messageId]
    val content = message[Messages.content]
    val sentBy = message[Messages.sentBy]
    val timestamp = message[Messages.timestamp]
    log.info(
        "Message retrieved from database: $uuid, content: $content, sentBy: $sentBy, timestamp: $timestamp"
    )
  }
}

@OptIn(ExperimentalUuidApi::class)
object Messages : Table("messages") {
  val messageId = uuid("message_id").uniqueIndex()
  val content = text("content")
  val timestamp = timestamp("timestamp")
  val sentBy = text("sent_by")
}

@Serializable
data class Message(
    val messageId: String,
    val content: String,
    val timestamp: Instant,
    val sentBy: Username,
)
