package com.example.com

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

private const val SYSTEM_USER = "SYSTEM"

fun Application.configureRouting() {
  routing {
    get("/") { call.respondText("Hello, World!") }
    webSocket("/ws") { SocketService.acceptConnection(this) }
  }
}
