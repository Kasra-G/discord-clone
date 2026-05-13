package com.example.com

import io.ktor.serialization.deserialize
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalAtomicApi::class)
fun Application.configureRouting() {
  val messageResponseFlow = MutableSharedFlow<UserBroadcastMessage>()
  val sharedFlow = messageResponseFlow.asSharedFlow()
  val counter = AtomicInt(0)
  routing {
    get("/") { call.respondText("Hello, World!") }
    webSocket("/ws") {
      val userId = counter.fetchAndIncrement()
      val username = "User-$userId"
      log.info("connected $username")

      sendSerialized(
          UserBroadcastMessage(
              username = "SYSTEM",
              message = "Welcome $username, you have connected to the chat room",
          )
      )
      messageResponseFlow.emit(
          UserBroadcastMessage(
              username = "SYSTEM",
              message = "$username has joined the chat room",
          )
      )

      val job = launch { sharedFlow.collect { sendSerialized(it) } }

      runCatching {
            val converter = requireNotNull(this.converter)
            incoming.consumeEach { frame ->
              if (frame !is Frame.Text) return@consumeEach
              val messageRequest = converter.deserialize<UserMessageRequest>(frame)

              val broadcastMessage =
                  UserBroadcastMessage(
                      username = username,
                      message = messageRequest.message,
                  )
              log.info("Broadcasting $broadcastMessage")
              messageResponseFlow.emit(broadcastMessage)
            }
          }
          .onFailure { exception -> println("WebSocket exception: ${exception.localizedMessage}") }
          .also {
            job.cancel()
            messageResponseFlow.emit(
                UserBroadcastMessage(
                    username = "SYSTEM",
                    message = "$username has left the chat room",
                )
            )
          }
    }
  }
}

@Serializable data class UserMessageRequest(val message: String)

@Serializable
data class UserBroadcastMessage(
    val username: String,
    val message: String,
    val receivedAt: Instant = Clock.System.now(),
)
