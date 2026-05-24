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
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable

@OptIn(ExperimentalAtomicApi::class)
class SocketService(val messageRepository: MessageRepository) {
  private val userCounter = AtomicInt(0)
  private val sessionMap = mutableMapOf<Username, WebSocketServerSession>()

  suspend fun acceptConnection(session: WebSocketServerSession) {
    val username = getNewUsername()
    // todo: replace the system messages with status indicator commands
    session.application.log.info("connected $username")
    //    session.sendChannelMessage(
    //        sender = Username.SYSTEM,
    //        message = "${username.value} has joined the chat room",
    //    )
    sessionMap[username] = session
    //    sendChannelMessageToRecipient(
    //        sender = Username.SYSTEM,
    //        recipient = username,
    //        message = "Welcome ${username.value}, you have connected to the chat room",
    //    )
    runCatching { session.handleIncomingUserFrames(username) }
        .onFailure { exception ->
          session.application.log.error(
              "WebSocket exception: ${exception.localizedMessage}",
              exception,
          )
        }
        .also {
          //          session.sendChannelMessage(
          //              sender = Username.SYSTEM,
          //              message = "${username.value} has left the chat room",
          //          )
        }
  }

  private suspend fun WebSocketServerSession.handleIncomingUserFrames(
      username: Username,
  ) {
    val log = application.log
    val converter = requireNotNull(converter)
    incoming.consumeEach { frame ->
      if (frame !is Frame.Text) return@consumeEach

      log.info(frame.readText())
      when (val incomingCommand = converter.deserialize<ServerCommand>(frame)) {
        is ServerCommand.BroadcastMessage -> {
          sendChannelMessage(sender = username, message = incomingCommand.message)
        }
        is ServerCommand.PrivateMessage -> {
          log.info("Unsupported command")
        }
      }
    }
  }

  private suspend fun WebSocketServerSession.sendChannelMessage(sender: Username, message: String) =
      supervisorScope {
        val saved =
            messageRepository.save(
                SaveMessageRequest(
                    channelId = ChannelId.DEFAULT,
                    content = message,
                    sentBy = sender,
                )
            )
        application.log.info("Saved message $saved")
        val commandToSend = ClientCommand.NewMessage(payload = saved)
        sessionMap.values.forEach { launch { it.sendSerialized(commandToSend) } }
      }

  //  private suspend fun sendChannelMessageToRecipient(
  //      sender: Username,
  //      recipient: Username,
  //      message: String,
  //  ) = coroutineScope {
  //    val commandToSend = ClientCommand.NewMessage(payload = Message())
  //    val receiverSession = sessionMap[recipient]
  //    checkNotNull(receiverSession) { "Recipient $recipient not found" }
  //    receiverSession.sendSerialized(commandToSend)
  //  }

  private fun getNewUsername() = Username("User-${userCounter.fetchAndIncrement()}")
}

@Serializable
@JvmInline
value class Username(val value: String) {

  companion object {
    val SYSTEM = Username("SYSTEM")
  }
}
