package com.ghkasra.discordclone

import com.ghkasra.discordclone.service.MessageRepository
import com.ghkasra.discordclone.service.SocketService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureRouting() {
  val db = Database.connect("jdbc:sqlite:chat.db")
  val repo = MessageRepository(db)
  val socketService = SocketService(repo)
  routing {
    get("/") { call.respondText("Hello, World!") }
    webSocket("/ws") { socketService.acceptConnection(this) }
  }
}
