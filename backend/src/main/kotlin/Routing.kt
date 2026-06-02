package com.ghkasra.discordclone

import at.favre.lib.crypto.bcrypt.BCrypt
import com.ghkasra.discordclone.repository.ChannelId
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.MessageRepository
import com.ghkasra.discordclone.repository.RefreshTokenRepository
import com.ghkasra.discordclone.repository.UserCredentialRepository
import com.ghkasra.discordclone.repository.UserId
import com.ghkasra.discordclone.repository.UserRepository
import com.ghkasra.discordclone.service.AccessToken
import com.ghkasra.discordclone.service.AuthenticationService
import com.ghkasra.discordclone.service.AuthenticationService.Companion.setAuthorizationCookies
import com.ghkasra.discordclone.service.BearerTokens
import com.ghkasra.discordclone.service.HashUtil
import com.ghkasra.discordclone.service.PasswordUtil
import com.ghkasra.discordclone.service.RefreshToken
import com.ghkasra.discordclone.service.RefreshTokenFactory
import com.ghkasra.discordclone.service.SocketService
import com.ghkasra.discordclone.service.UserLoginRequest
import com.ghkasra.discordclone.service.UserRegistrationRequest
import com.ghkasra.discordclone.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.requireCookie
import io.ktor.server.request.requirePathParameter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureRouting() {
  install(IgnoreTrailingSlash)
  val db = Database.connect(getEnvOrThrow("DATABASE_URL"))
  val msgRepo = MessageRepository(db)
  val userRepo = UserRepository(db)
  val refreshTokenRepo = RefreshTokenRepository(db)
  val passwordUtil = PasswordUtil(BCrypt.withDefaults())
  val hashUtil = HashUtil(MessageDigest.getInstance("SHA-256"))
  val tokenFactory = RefreshTokenFactory()
  val userCredentialRepo = UserCredentialRepository(db)
  val authService =
      AuthenticationService(
          credentialsRepository = userCredentialRepo,
          refreshTokenRepository = refreshTokenRepo,
          passwordUtil = passwordUtil,
          hashUtil = hashUtil,
          tokenFactory = tokenFactory,
      )
  val userService = UserService(authService, userRepo)
  val socketService =
      SocketService(
          messageRepository = msgRepo,
          userRepository = userRepo,
      )

  routing {
    route("/api") {
      post("/users/register") {
        val request = call.receive<UserRegistrationRequest>()
        val response = userService.register(request)
        call.respond(HttpStatusCode.Created, response)
      }
      post("/users/login") {
        val request = call.receive<UserLoginRequest>()
        val response = userService.login(request)
        setAuthorizationCookies(BearerTokens(response.refreshToken, response.accessToken))
        call.respond(
            HttpStatusCode.OK,
            response,
        )
      }
      post("/auth/refresh") {
        val request = call.receive<AccessTokenRefreshRequest>()
        val refreshToken =
            RefreshToken(call.requireCookie(AuthenticationService.REFRESH_TOKEN_COOKIE_NAME))
        val response = authService.generateAccessToken(refreshToken, request.deviceId)
        setAuthorizationCookies(BearerTokens(response.refreshToken, response.accessToken))
        call.respond(
            HttpStatusCode.OK,
            AccessTokenRefreshResponse(response.refreshToken, response.accessToken),
        )
      }
      authenticate {
        post("/auth/revoke") {
          val claim = call.retrieveAuthenticatedClaims()
          refreshTokenRepo.revokeAll(claim.userId)
          call.respond(HttpStatusCode.OK)
        }
        get("/hello") {
          val claims = call.retrieveAuthenticatedClaims()
          val expiresIn = claims.expiresAt.minus(Clock.System.now())
          val userDetails = userRepo.get(claims.userId)

          call.respondText(
              "Hello, ${userDetails.username}! Token expires in ${expiresIn.toString(DurationUnit.MILLISECONDS)} ms."
          )
        }
        post("/channels/{channelId}/messages") {
          val request = call.receive<CreateMessageRequest>()
          val claims = call.retrieveAuthenticatedClaims()
          val message =
              msgRepo.save(
                  channelId = request.channelId,
                  content = request.message,
                  authorId = claims.userId,
              )
          launch { socketService.sendChannelMessage(message) }
          call.respond(HttpStatusCode.Created)
        }
        get("/channels/{channelId}/messages") {
          val channelId = ChannelId(call.requirePathParameter("channelId"))
          val count = call.queryParameters["count"]?.toIntOrNull() ?: 10
          require(count > -1) { "Count must be positive" }
          require(count < 100) { "Count must be less than 100" }
          call.respond(msgRepo.listMessages(channelId, count))
        }
      }
    }
    get("/") { call.respondText("Hello, World!") }
    authenticate {
      webSocket("/ws") {
        val claims = call.retrieveAuthenticatedClaims()
        socketService.acceptConnection(this, claims.userId)
      }
    }
  }
}

data class UserClaims(val userId: UserId, val expiresAt: Instant)

@OptIn(ExperimentalUuidApi::class)
fun ApplicationCall.retrieveAuthenticatedClaims(): UserClaims {
  val principal = principal<JWTPrincipal>()
  checkNotNull(principal) { "Call JWTPrincipal is null" }
  val subject = principal.payload.subject
  val expiresAt = principal.expiresAt
  checkNotNull(subject) { "Subject is null" }
  checkNotNull(expiresAt) { "Expires At is null" }

  return UserClaims(
      UserId(Uuid.parse(subject)),
      expiresAt = expiresAt.toInstant().toKotlinInstant(),
  )
}

@Serializable data class CreateMessageRequest(val channelId: ChannelId, val message: String)

@Serializable data class AccessTokenRefreshRequest(val deviceId: DeviceId)

@Serializable
data class AccessTokenRefreshResponse(val refreshToken: RefreshToken, val accessToken: AccessToken)
