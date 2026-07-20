package com.ghkasra.discordclone.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ghkasra.discordclone.Environment
import com.ghkasra.discordclone.model.ServiceException
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.PasswordHash
import com.ghkasra.discordclone.repository.RefreshTokenCredentials
import com.ghkasra.discordclone.repository.RefreshTokenHash
import com.ghkasra.discordclone.repository.RefreshTokenRepository
import com.ghkasra.discordclone.repository.TokenNotFoundException
import com.ghkasra.discordclone.repository.UserCredentialRepository
import com.ghkasra.discordclone.repository.UserId
import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import com.sksamuel.aedile.core.expireAfterWrite
import io.ktor.server.routing.RoutingContext
import io.ktor.server.util.toGMTDate
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.Logger

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

  val refreshCache =
      Caffeine.newBuilder()
          .expireAfterWrite(5.seconds)
          .maximumSize(1_000)
          .asCache<RefreshTokenHash, BearerTokens>()

  @WithSpan
  suspend fun authenticateByUserLogin(userLogin: UserLogin): Boolean {
    val credentials =
        credentialsRepository.findByUsername(userLogin.username)
            ?: throw ServiceException.Unauthorized("Invalid username or password")

    return withContext(Dispatchers.Default + Context.current().asContextElement()) {
      passwordUtil.verifyPassword(userLogin.password, credentials.passwordHash)
    }
  }

  @WithSpan
  fun storeUserLoginCredentials(userLogin: UserLogin) {
    credentialsRepository.create(userLogin.username, passwordUtil.hashPassword(userLogin.password))
  }

  @WithSpan
  suspend fun issueInitialBearerTokens(userId: UserId, deviceId: DeviceId): BearerTokens {
    val refreshToken = tokenFactory.create()
    val accessToken = createAccessToken(userId)
    val refreshTokenHash = hashRefreshToken(refreshToken)

    withContext(Context.current().asContextElement()) {
      suspendTransaction() {
        refreshTokenRepository.revokeExistingDeviceTokens(userId, deviceId)
        refreshTokenRepository.save(
            tokenHash = refreshTokenHash,
            userId = userId,
            deviceId = deviceId,
            expiresIn = REFRESH_TOKEN_DURATION,
        )
      }
    }

    return BearerTokens(
        refreshToken = refreshToken,
        accessToken = accessToken,
    )
  }

  @WithSpan
  context(logger: Logger)
  suspend fun refresh(token: RefreshToken): BearerTokens {
    val tokenHash = hashRefreshToken(token)

    val newToken = tokenFactory.create()
    val newTokenHash = hashRefreshToken(newToken)

    try {
      return refreshCache.get(tokenHash) {
        logger.info("Cache miss! $token")
        withContext(Dispatchers.IO + Context.current().asContextElement()) {
          val existingToken = suspendTransaction {
            refreshTokenRepository.findValidTokenByHash(tokenHash).getOrThrow().apply {
              refreshTokenRepository.markAsUsed(tokenHash)
              refreshTokenRepository.save(
                  tokenHash = newTokenHash,
                  userId = userId,
                  deviceId = deviceId,
                  expiresIn = REFRESH_TOKEN_DURATION,
              )
            }
          }
          BearerTokens(
                  refreshToken = newToken,
                  accessToken = createAccessToken(existingToken.userId),
              )
              .also {
                logger.info("Saving to cache! $token")
                refreshCache.put(tokenHash, it)
              }
        }
      }
    } catch (_: TokenNotFoundException) {
      logger.info("Could not find $token in db")
    }

    // check if we tried to use a refresh token that was already used
    try {
      withContext(Dispatchers.IO + Context.current().asContextElement()) {
        suspendTransaction {
          logger.info("Attempting to find used $token")
          val oldToken = refreshTokenRepository.findAnyTokenByHash(tokenHash).getOrThrow()
          logger.info("Found $oldToken")
          if (oldToken.isUsed) {
            logger.warn(
                "Refresh token id ${oldToken.id} compromised (Device id: ${oldToken.deviceId}, User id: ${oldToken.userId}); revoking all user tokens"
            )
            refreshTokenRepository.revokeAll(oldToken.userId)
          }
        }
      }
    } catch (_: TokenNotFoundException) {
      logger.info("Could not find used $token")
    }
    throw ServiceException.Unauthorized("Invalid refresh token")
  }

  private fun RefreshTokenCredentials.isInGracePeriod() =
      Clock.System.now().minus(REFRESH_GRACE_PERIOD_DURATION) < usedAt

  private fun hashRefreshToken(token: RefreshToken) = RefreshTokenHash(hashUtil.hash(token.value))

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
    val REFRESH_TOKEN_DURATION = 7.days
    val REFRESH_GRACE_PERIOD_DURATION = 5.seconds
    val ACCESS_TOKEN_DURATION = 5.minutes
    const val AUDIENCE = "http://localhost"
    const val ISSUER = "http://localhost"
    const val REALM = "Ktor Server"
    const val ACCESS_TOKEN_COOKIE_NAME = "access_token"
    const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

    fun RoutingContext.setAuthorizationCookies(tokens: BearerTokens) {
      call.response.cookies.append(
          name = REFRESH_TOKEN_COOKIE_NAME,
          value = tokens.refreshToken.value,
          httpOnly = true,
          domain = Environment.DOMAIN,
          expires = Clock.System.now().plus(REFRESH_TOKEN_DURATION).toJavaInstant().toGMTDate(),
          path = "/",
      )
      call.response.cookies.append(
          name = ACCESS_TOKEN_COOKIE_NAME,
          value = tokens.accessToken.value,
          httpOnly = true,
          domain = Environment.DOMAIN,
          expires = Clock.System.now().plus(ACCESS_TOKEN_DURATION).toJavaInstant().toGMTDate(),
          path = "/",
      )
    }
  }
}

class RefreshTokenFactory {
  private val secureRandom = SecureRandom()

  @WithSpan
  fun create(): RefreshToken {
    val randomBytes = ByteArray(TOKEN_BYTE_LENGTH)
    secureRandom.nextBytes(randomBytes)
    return RefreshToken(Base64.encode(randomBytes))
  }

  companion object {
    private const val TOKEN_BYTE_LENGTH = 32
  }
}

class HashUtil {
  @WithSpan fun hash(value: String): String = digest.digest(value.encodeToByteArray()).toHexString()

  private val proto = MessageDigest.getInstance("SHA-256")
  private val digest: MessageDigest
    get() = proto.clone() as MessageDigest
}

class PasswordUtil(val hasher: BCrypt.Hasher) {
  @WithSpan
  fun hashPassword(password: Password): PasswordHash {
    require(password.value.length <= 72) { "password must be less than 72 characters long!" }
    return PasswordHash(hasher.hashToString(10, password.value.toCharArray()))
  }

  @WithSpan
  fun verifyPassword(password: Password, passwordHash: PasswordHash): Boolean {
    return BCrypt.verifyer()
        .verify(password.value.toCharArray(), passwordHash.value.toCharArray())
        .verified
  }
}
