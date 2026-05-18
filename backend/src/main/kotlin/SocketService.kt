package com.example.com

import io.ktor.serialization.deserialize
import io.ktor.server.application.log
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.application
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.Frame
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable

@OptIn(ExperimentalAtomicApi::class)
object SocketService {
  private val userCounter = AtomicInt(0)
  private val sessionMap = mutableMapOf<User, WebSocketServerSession>()

  suspend fun acceptConnection(session: WebSocketServerSession) {
    val user = getNewUser()
    session.application.log.info("connected $user")
    sendChannelMessage(
        sender = User.SYSTEM,
        message = "${user.username} has joined the chat room",
    )
    sessionMap[user] = session
    sendChannelMessageToRecipient(
        sender = User.SYSTEM,
        recipient = user,
        message = "Welcome ${user.username}, you have connected to the chat room",
    )
    runCatching { handleIncomingUserFrames(session, user) }
        .onFailure { exception -> println("WebSocket exception: ${exception.localizedMessage}") }
        .also {
          sendChannelMessage(
              sender = user,
              message = "${user.username} has left the chat room",
          )
        }
  }

  private suspend fun handleIncomingUserFrames(session: WebSocketServerSession, user: User) {
    val converter = requireNotNull(session.converter)
    session.incoming.consumeEach { frame ->
      if (frame !is Frame.Text) return@consumeEach
      val incomingCommand = converter.deserialize<BroadcastMessageCommand>(frame)

      sendChannelMessage(sender = user, message = incomingCommand.message)
    }
  }

  private suspend fun sendChannelMessage(sender: User, message: String) = supervisorScope {
    val messageToSend = ChannelMessage(sender.username, message)
    sessionMap.values.forEach { launch { it.sendSerialized(messageToSend) } }
  }

  private suspend fun sendChannelMessageToRecipient(
      sender: User,
      recipient: User,
      message: String,
  ) = supervisorScope {
    val messageToSend = ChannelMessage(sender.username, message)
    val receiverSession = sessionMap[recipient]
    checkNotNull(receiverSession) { "Recipient $recipient not found" }
    receiverSession.sendSerialized(messageToSend)
  }

  private fun getNewUser() = User.create("User-${userCounter.fetchAndIncrement()}")
}

class User private constructor(val username: String) {
  init {
    require(username.isNotEmpty())
  }

  companion object {
    val SYSTEM = User("SYSTEM")

    fun create(username: String): User {
      require(username != SYSTEM.username) { "Cannot use reserved username ${SYSTEM.username}" }
      return User(username)
    }
  }
}

@Serializable data class BroadcastMessageCommand(val message: String)

@Serializable
data class ChannelMessage(
    val sender: String,
    val message: String,
    val timestamp: Instant = Clock.System.now(),
)
