package com.example.com

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalAtomicApi::class)
fun Application.configureRouting() {
  val messageResponseFlow = MutableSharedFlow<String>()
  val sharedFlow = messageResponseFlow.asSharedFlow()
  val counter = AtomicInt(0)
  routing {
    get("/") { call.respondText("Hello, World!") }
    webSocket("/ws") {
      val userId = counter.fetchAndIncrement()
      log.info("connected $userId")
      send("SYSTEM: Welcome User-$userId, you have connected to the chat room")
      messageResponseFlow.emit("User-$userId: has joined the chat room")

      val job = launch { sharedFlow.collect { message -> send(message) } }

      runCatching {
            incoming.consumeEach { frame ->
              if (frame is Frame.Text) {
                val message = frame.readText()
                log.info("Broadcasting message $message by user $userId")
                messageResponseFlow.emit("User-$userId: $message")
              }
            }
          }
          .onFailure { exception -> println("WebSocket exception: ${exception.localizedMessage}") }
          .also { job.cancel() }
    }
  }
}
