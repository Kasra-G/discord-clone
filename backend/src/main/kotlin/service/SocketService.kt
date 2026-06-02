package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ClientCommand
import com.ghkasra.discordclone.repository.Message
import com.ghkasra.discordclone.repository.MessageRepository
import com.ghkasra.discordclone.repository.User
import com.ghkasra.discordclone.repository.UserId
import com.ghkasra.discordclone.repository.UserRepository
import io.ktor.server.application.log
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.application
import io.ktor.server.websocket.sendSerialized
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable

@Serializable data class WebsocketClientInitiation(val token: AccessToken)

class SocketService(
    val messageRepository: MessageRepository,
    val userRepository: UserRepository,
) {
  private val sessionMap =
      ConcurrentHashMap<
          UserId,
          CopyOnWriteArrayList<WebSocketServerSession>,
      >()

  suspend fun acceptConnection(session: WebSocketServerSession, userId: UserId) {

    val user = userRepository.get(userId)
    val username = user.username
    sessionMap.getOrPut(user.id) { CopyOnWriteArrayList() }.add(session)
    // todo: replace the system messages with status indicator commands
    session.application.log.info("connected $username")
    //    session.sendChannelMessage(
    //        sender = Username.SYSTEM,
    //        message = "${username.value} has joined the chat room",
    //    )
    //    sendChannelMessageToRecipient(
    //        sender = Username.SYSTEM,
    //        recipient = username,
    //        message = "Welcome ${username.value}, you have connected to the chat room",
    //    )
    session
        .runCatching { handleIncomingUserFrames(user) }
        .onFailure { exception ->
          session.application.log.error(
              "WebSocket exception: ${exception.localizedMessage}",
              exception,
          )
        }
        .also {
          val userSessionList = sessionMap[user.id]
          checkNotNull(userSessionList)
          userSessionList.remove(session)
          //          session.sendChannelMessage(
          //              sender = Username.SYSTEM,
          //              message = "${username.value} has left the chat room",
          //          )
        }
  }

  private suspend fun WebSocketServerSession.handleIncomingUserFrames(
      user: User,
  ) {
    incoming.consumeEach {}
  }

  suspend fun sendChannelMessage(
      message: Message,
  ) = supervisorScope {
    val commandToSend = ClientCommand.NewMessage(payload = message)
    sessionMap.values.forEach { userSessions ->
      userSessions.forEach { launch { it.sendSerialized(commandToSend) } }
    }
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
}

@Serializable @JvmInline value class Username(val value: String)
