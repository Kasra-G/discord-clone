package com.ghkasra.discordclone

import at.favre.lib.crypto.bcrypt.BCrypt
import com.ghkasra.discordclone.repository.ChannelId
import com.ghkasra.discordclone.repository.ChannelRepository
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.GuildId
import com.ghkasra.discordclone.repository.GuildMemberRepository
import com.ghkasra.discordclone.repository.GuildRepository
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
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureRouting() {
  install(IgnoreTrailingSlash)
  val db = Database.connect(Environment.DATABASE_URL)
  val msgRepo = MessageRepository(db)
  val userRepo = UserRepository(db)
  val guildRepo = GuildRepository(db)
  val guildMemberRepo = GuildMemberRepository(db)
  val channelRepo = ChannelRepository(db)
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
          userRepository = userRepo,
      )

  @Serializable data class HealthCheckResponse(val status: String, val timestamp: Instant)

  context(log) {
    routing {
      route("/api") {
        get("/health") {
          call.respond(HttpStatusCode.OK, HealthCheckResponse("ok", Clock.System.now()))
        }
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

          route("/guilds/{guildId}") {
            get {
              val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
              call.respond(HttpStatusCode.OK, guildRepo.get(guildId))
            }
            delete {
              val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
              call.respond(HttpStatusCode.OK, guildRepo.delete(guildId))
            }

            route("/channels") {
              get {
                val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                val channels = channelRepo.list(guildId)
                call.respond(HttpStatusCode.OK, channels)
              }
              post {
                val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                val createChannelRequest = call.receive<ChannelCreateRequest>()
                val createdChannel =
                    channelRepo.create(
                        guildId = guildId,
                        name = createChannelRequest.name,
                        description = createChannelRequest.description,
                    )
                call.respond(HttpStatusCode.Created, createdChannel)
              }
            }

            route("/members") {
              get {
                val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                val guildMembers = guildMemberRepo.listGuildMembers(guildId)
                call.respond(HttpStatusCode.OK, guildMembers)
              }
              route("/{userId}") {
                get {
                  val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                  val userId = UserId(Uuid.parse(call.requirePathParameter("userId")))
                  val guildMember = guildMemberRepo.get(guildId, userId)
                  call.respond(HttpStatusCode.OK, guildMember)
                }
                post {
                  val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                  val userId = UserId(Uuid.parse(call.requirePathParameter("userId")))
                  guildMemberRepo.create(guildId, userId)
                  call.respond(HttpStatusCode.NoContent)
                }
                delete {
                  val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                  val userId = UserId(Uuid.parse(call.requirePathParameter("userId")))
                  guildMemberRepo.delete(guildId, userId)
                  call.respond(HttpStatusCode.NoContent)
                }
              }
            }
          }
          route("/channels/{channelId}") {
            get {
              val channelId = ChannelId(Uuid.parse(call.requirePathParameter("channelId")))
              call.respond(HttpStatusCode.OK, channelRepo.get(channelId))
            }

            route("/messages") {
              post {
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
              get {
                val channelId = ChannelId(Uuid.parse(call.requirePathParameter("channelId")))
                val count = call.queryParameters["count"]?.toIntOrNull() ?: 10
                require(count > -1) { "Count must be positive" }
                require(count <= 100) { "Count must be less than equal to 100" }
                call.respond(msgRepo.listMessages(channelId, count))
              }
            }
          }
          route("/users") {
            route("/@me") {
              get {
                val claims = call.retrieveAuthenticatedClaims()
                val user = userRepo.get(claims.userId)
                call.respond(HttpStatusCode.OK, user)
              }
              get("/guilds") {
                val claims = call.retrieveAuthenticatedClaims()
                val guilds = guildMemberRepo.listUserGuilds(claims.userId)
                call.respond(HttpStatusCode.OK, guilds)
              }
              post("/guilds") {
                val claims = call.retrieveAuthenticatedClaims()
                val createGuildRequest = call.receive<CreateGuildRequest>()
                val guild =
                    guildRepo.create(
                        claims.userId,
                        name = createGuildRequest.name,
                        description = createGuildRequest.description,
                    )
                guildMemberRepo.create(guild.id, claims.userId)
                channelRepo.create(guild.id, "welcome", "Default channel")
                call.respond(HttpStatusCode.Created, guild)
              }
              post("/guilds/{guildId}/members") {
                val claims = call.retrieveAuthenticatedClaims()
                val guildId = GuildId(Uuid.parse(call.requirePathParameter("guildId")))
                guildMemberRepo.create(guildId, claims.userId)
                call.respond(HttpStatusCode.NoContent)
              }
            }
            get("{userId}") {
              val userId = UserId(Uuid.parse(call.requirePathParameter("userId")))
              val user = userRepo.get(userId)
              call.respond(HttpStatusCode.OK, user)
            }
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
}

@Serializable
data class ChannelCreateRequest(
    val name: String,
    val description: String,
)

data class UserClaims(val userId: UserId, val expiresAt: Instant)

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

@Serializable data class CreateGuildRequest(val name: String, val description: String)

@Serializable data class CreateMessageRequest(val channelId: ChannelId, val message: String)

@Serializable data class AccessTokenRefreshRequest(val deviceId: DeviceId)

@Serializable
data class AccessTokenRefreshResponse(val refreshToken: RefreshToken, val accessToken: AccessToken)
