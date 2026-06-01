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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable

@Serializable data class WebsocketClientInitiation(val userId: UserId)

class SocketService(val messageRepository: MessageRepository, val userRepository: UserRepository) {
  private val sessionMap = ConcurrentHashMap<UserId, CopyOnWriteArrayList<WebSocketServerSession>>()

  suspend fun WebSocketServerSession.ignoreUntilAuthenticated(): User {
    val converter = checkNotNull(converter)

    for (frame in incoming) {
      if (frame is Frame.Text) {
        val init =
            runCatching { converter.deserialize<WebsocketClientInitiation>(frame) }
                .getOrElse { continue }
        val userId = init.userId
        application.log.info("Authorized user $userId")
        return userRepository.get(GetUserRequest(userId))
      }
    }
    throw IllegalArgumentException("Connection closed before authenticating")
  }

  suspend fun acceptConnection(session: WebSocketServerSession) {
    val user = session.ignoreUntilAuthenticated()
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
    val log = application.log
    val converter = requireNotNull(converter)
    incoming.consumeEach { frame ->
      if (frame !is Frame.Text) return@consumeEach

      log.info(frame.readText())
      val incomingCommand =
          runCatching { converter.deserialize<ServerCommand>(frame) }
              .getOrElse {
                log.error("Unknown server command ${frame.readText()}")
                return@consumeEach
              }
      when (incomingCommand) {
        is ServerCommand.BroadcastMessage -> {
          sendChannelMessage(author = user, message = incomingCommand.message)
        }
        is ServerCommand.PrivateMessage -> {
          log.info("Unsupported command")
        }
      }
    }
  }

  private suspend fun WebSocketServerSession.sendChannelMessage(author: User, message: String) =
      supervisorScope {
        val saved =
            messageRepository.save(
                SaveMessageRequest(
                    channelId = ChannelId.DEFAULT,
                    content = message,
                    authorId = author.id,
                )
            )
        application.log.info("Saved message $saved")
        val commandToSend = ClientCommand.NewMessage(payload = saved)
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
