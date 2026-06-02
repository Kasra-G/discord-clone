package com.ghkasra.discordclone.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ghkasra.discordclone.Environment
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.PasswordHash
import com.ghkasra.discordclone.repository.RefreshTokenHash
import com.ghkasra.discordclone.repository.RefreshTokenRepository
import com.ghkasra.discordclone.repository.UserCredentialRepository
import com.ghkasra.discordclone.repository.UserId
import io.ktor.server.routing.RoutingContext
import io.ktor.server.util.toGMTDate
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.Serializable

@Serializable @JvmInline value class Password(val value: String)

@Serializable @JvmInline value class AccessToken(val value: String)

@Serializable @JvmInline value class RefreshToken(val value: String)

data class UserLogin(val username: Username, val password: Password)

data class BearerTokens(
    val refreshToken: RefreshToken,
    val accessToken: AccessToken,
)

class AuthenticationService(
    val credentialsRepository: UserCredentialRepository,
    val refreshTokenRepository: RefreshTokenRepository,
    val passwordUtil: PasswordUtil,
    val hashUtil: HashUtil,
    val tokenFactory: RefreshTokenFactory,
) {

  fun authenticateByUserLogin(userLogin: UserLogin): Boolean {
    val credentials = credentialsRepository.findByUsername(userLogin.username)
    requireNotNull(credentials) { "User does not have credentials" }

    return passwordUtil.verifyPassword(userLogin.password, credentials.passwordHash)
  }

  fun storeUserLoginCredentials(userLogin: UserLogin) {
    credentialsRepository.create(userLogin.username, passwordUtil.hashPassword(userLogin.password))
  }

  fun issueRefreshToken(userId: UserId, deviceId: DeviceId): RefreshToken {
    val refreshToken = tokenFactory.create()
    refreshTokenRepository.save(
        tokenHash = hashRefreshToken(refreshToken),
        userId = userId,
        deviceId = deviceId,
        expiresIn = REFRESH_TOKEN_DURATION,
    )
    return refreshToken
  }

  fun generateAccessToken(token: RefreshToken, deviceId: DeviceId): BearerTokens {
    val tokenCredentials = refreshTokenRepository.findByTokenHash(hashRefreshToken(token))
    requireNotNull(tokenCredentials) { "Refresh token does not exist" }
    return BearerTokens(
        token,
        createAccessToken(tokenCredentials.userId),
    )
  }

  private fun hashRefreshToken(token: RefreshToken) =
      RefreshTokenHash(hashUtil.hash(hashUtil.hash(token.value)))

  @OptIn(ExperimentalUuidApi::class)
  private fun createAccessToken(userId: UserId) =
      AccessToken(
          JWT.create()
              .withAudience(AUDIENCE)
              .withIssuer(ISSUER)
              .withSubject(userId.value.toString())
              .withExpiresAt(Clock.System.now().plus(ACCESS_TOKEN_DURATION).toJavaInstant())
              .sign(Algorithm.HMAC256(Environment.JWT_SECRET))
      )

  companion object {
    val REFRESH_TOKEN_DURATION = 7.toDuration(DurationUnit.DAYS)
    val ACCESS_TOKEN_DURATION = 5.toDuration(DurationUnit.MINUTES)
    const val AUDIENCE = "http://localhost:5173"
    const val ISSUER = "http://localhost:8080"
    const val REALM = "Ktor Server"
    const val ACCESS_TOKEN_COOKIE_NAME = "access_token"
    const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

    fun RoutingContext.setAuthorizationCookies(tokens: BearerTokens) {
      call.response.cookies.append(
          name = REFRESH_TOKEN_COOKIE_NAME,
          value = tokens.refreshToken.value,
          httpOnly = true,
          expires = Clock.System.now().plus(REFRESH_TOKEN_DURATION).toJavaInstant().toGMTDate(),
          path = "/",
      )
      call.response.cookies.append(
          name = ACCESS_TOKEN_COOKIE_NAME,
          value = tokens.accessToken.value,
          httpOnly = true,
          expires = Clock.System.now().plus(ACCESS_TOKEN_DURATION).toJavaInstant().toGMTDate(),
          path = "/",
      )
    }
  }
}

class RefreshTokenFactory {
  private val secureRandom = SecureRandom()

  fun create(): RefreshToken {
    val randomBytes = ByteArray(TOKEN_BYTE_LENGTH)
    secureRandom.nextBytes(randomBytes)
    return RefreshToken(Base64.encode(randomBytes))
  }

  companion object {
    private const val TOKEN_BYTE_LENGTH = 32
  }
}

class HashUtil(val digest: MessageDigest) {
  //    val digest = MessageDigest.getInstance("SHA-256")
  fun hash(value: String): String = digest.digest(value.encodeToByteArray()).toHexString()
}

class PasswordUtil(val hasher: BCrypt.Hasher) {
  fun hashPassword(password: Password): PasswordHash {
    require(password.value.length <= 72) { "password must be less than 72 characters long!" }
    return PasswordHash(hasher.hashToString(10, password.value.toCharArray()))
  }

  fun verifyPassword(password: Password, passwordHash: PasswordHash): Boolean {
    return BCrypt.verifyer()
        .verify(password.value.toCharArray(), passwordHash.value.toCharArray())
        .verified
  }
}
