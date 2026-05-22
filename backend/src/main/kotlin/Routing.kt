package com.ghkasra.discordclone

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun Application.configureRouting() {
  routing {
    get("/") { call.respondText("Hello, World!") }
    webSocket("/ws") { SocketService.acceptConnection(this) }
  }
}
