package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ChannelId
import com.ghkasra.discordclone.model.ClientCommand
import com.ghkasra.discordclone.model.SaveMessageRequest
import com.ghkasra.discordclone.model.ServerCommand
import io.ktor.serialization.deserialize
import io.ktor.server.application.log
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.application
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable

@OptIn(ExperimentalAtomicApi::class)
class SocketService(val messageRepository: MessageRepository) {
  private val userCounter = AtomicInt(0)
  private val sessionMap = mutableMapOf<Username, WebSocketServerSession>()

  suspend fun acceptConnection(session: WebSocketServerSession) {
    val username = getNewUsername()
    session.application.log.info("connected $username")
    sendChannelMessage(
        sender = Username.SYSTEM,
        message = "${username.value} has joined the chat room",
    )
    sessionMap[username] = session
    sendChannelMessageToRecipient(
        sender = Username.SYSTEM,
        recipient = username,
        message = "Welcome ${username.value}, you have connected to the chat room",
    )
    runCatching { handleIncomingUserFrames(session, username) }
        .onFailure { exception ->
          session.application.log.error(
              "WebSocket exception: ${exception.localizedMessage}",
              exception,
          )
        }
        .also {
          sendChannelMessage(
              sender = Username.SYSTEM,
              message = "${username.value} has left the chat room",
          )
        }
  }

  private suspend fun handleIncomingUserFrames(
      session: WebSocketServerSession,
      username: Username,
  ) {
    val log = session.application.log
    val converter = requireNotNull(session.converter)
    session.incoming.consumeEach { frame ->
      if (frame !is Frame.Text) return@consumeEach

      log.info(frame.readText())
      when (val incomingCommand = converter.deserialize<ServerCommand>(frame)) {
        is ServerCommand.BroadcastMessage -> {
          val content = incomingCommand.message
          log.info("Saving message $content from user $username")
          val saved =
              messageRepository.save(
                  SaveMessageRequest(
                      channelId = ChannelId.DEFAULT,
                      content = content,
                      sentBy = username,
                  )
              )
          log.info("Saved message $saved")
          sendChannelMessage(sender = username, message = content)
        }
        is ServerCommand.PrivateMessage -> {
          sendChannelMessageToRecipient(
              sender = username,
              Username(incomingCommand.recipient),
              incomingCommand.message,
          )
        }
      }
    }
  }

  private suspend fun sendChannelMessage(sender: Username, message: String) = supervisorScope {
    val commandToSend = ClientCommand.NewMessage(message = message, sender = sender)
    sessionMap.values.forEach { launch { it.sendSerialized<ClientCommand>(commandToSend) } }
  }

  private suspend fun sendChannelMessageToRecipient(
      sender: Username,
      recipient: Username,
      message: String,
  ) = coroutineScope {
    val commandToSend = ClientCommand.NewMessage(message = message, sender = sender)
    val receiverSession = sessionMap[recipient]
    checkNotNull(receiverSession) { "Recipient $recipient not found" }
    receiverSession.sendSerialized<ClientCommand>(commandToSend)
  }

  private fun getNewUsername() = Username("User-${userCounter.fetchAndIncrement()}")
}

@Serializable
@JvmInline
value class Username(val value: String) {

  companion object {
    val SYSTEM = Username("SYSTEM")
  }
}
