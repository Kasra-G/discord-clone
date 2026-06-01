package com.ghkasra.discordclone

import com.ghkasra.discordclone.model.ChannelId
import com.ghkasra.discordclone.service.CreateUserRequest
import com.ghkasra.discordclone.service.MessageRepository
import com.ghkasra.discordclone.service.SocketService
import com.ghkasra.discordclone.service.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureRouting() {
  install(IgnoreTrailingSlash)
  val db = Database.connect(getEnvOrThrow("DATABASE_URL"))
  val msgRepo = MessageRepository(db)
  val userRepo = UserRepository(db)
  val socketService =
      SocketService(
          messageRepository = msgRepo,
          userRepository = userRepo,
      )
  routing {
    post("/users/register") {
      val createUserRequest = call.receive<CreateUserRequest>()
      val createdUser = userRepo.save(createUserRequest)
      application.log.info("User created: $createdUser")
      call.respond(HttpStatusCode.Created, createdUser)
    }
    post("/users/login") { call.respond(Response("OK")) }
    get("/") { call.respondText("Hello, World!") }
    get("/channels/{channelId}/messages") {
      val channelId = ChannelId(call.requirePathParameter("channelId"))
      val count = call.queryParameters["count"]?.toIntOrNull() ?: 10
      require(count > -1) { "Count must be positive" }
      require(count < 100) { "Count must be less than 100" }
      call.respond(msgRepo.listMessages(channelId, count))
    }
    webSocket("/ws") { socketService.acceptConnection(this) }
  }
}

@Serializable data class Response(val response: String)
