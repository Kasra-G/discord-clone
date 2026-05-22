package com.ghkasra.discordclone

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
object SocketService {
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
              sender = username,
              message = "${username.value} has left the chat room",
          )
        }
  }

  private suspend fun handleIncomingUserFrames(
      session: WebSocketServerSession,
      username: Username,
  ) {
    val converter = requireNotNull(session.converter)
    session.incoming.consumeEach { frame ->
      if (frame !is Frame.Text) return@consumeEach

      session.application.log.info(frame.readText())
      when (val incomingCommand = converter.deserialize<ServerCommand>(frame)) {
        is ServerCommand.BroadcastMessage ->
            sendChannelMessage(sender = username, message = incomingCommand.message)
        is ServerCommand.PrivateMessage ->
            sendChannelMessageToRecipient(
                sender = username,
                Username.create(incomingCommand.recipient),
                incomingCommand.message,
            )
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

  private fun getNewUsername() = Username.create("User-${userCounter.fetchAndIncrement()}")
}

@Serializable
@JvmInline
value class Username private constructor(val value: String) {
  init {
    require(value.isNotEmpty())
  }

  companion object {
    val SYSTEM = Username("SYSTEM")

    fun create(username: String): Username {
      require(username != SYSTEM.value) { "Cannot use reserved username ${SYSTEM.value}" }
      return Username(username)
    }
  }
}
