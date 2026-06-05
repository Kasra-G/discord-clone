package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ServiceException
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.Email
import com.ghkasra.discordclone.repository.User
import com.ghkasra.discordclone.repository.UserId
import com.ghkasra.discordclone.repository.UserRepository
import kotlinx.serialization.Serializable
import org.slf4j.Logger

class UserService(val authService: AuthenticationService, val userRepository: UserRepository) {

  context(logger: Logger)
  fun register(request: UserRegistrationRequest): UserRegistrationResponse {
    logger.debug("Attempting to register user ${request.username}")
    val isUsernameAvailable = userRepository.findByUsername(request.username) == null

    require(isUsernameAvailable) { "Username ${request.username} is not available" }

    val userDetails =
        userRepository.create(
            username = request.username,
            email = request.email,
        )
    authService.storeUserLoginCredentials(UserLogin(request.username, request.password))

    logger.debug("Successfully registered user ${request.username}")
    return UserRegistrationResponse(userDetails.id)
  }

  context(logger: Logger)
  fun login(request: UserLoginRequest): UserLoginResponse {
    logger.debug("Attempting to log in user ${request.username}")
    val authenticated =
        authService.authenticateByUserLogin(UserLogin(request.username, request.password))

    if (!authenticated) {
      throw ServiceException.Unauthorized("Invalid username or password")
    }

    val userDetails = userRepository.findByUsername(request.username)

    checkNotNull(userDetails) {
      "Critical: Username ${request.username} has credentials but no user details!"
    }

    val refreshToken = authService.issueRefreshToken(userDetails.id, request.deviceId)
    val bearerTokens = authService.generateAccessToken(refreshToken, request.deviceId)
    logger.debug("Logged in user ${userDetails.username}")
    return UserLoginResponse(bearerTokens.refreshToken, bearerTokens.accessToken, userDetails)
  }
}

@Serializable
data class UserRegistrationRequest(val username: Username, val password: Password, val email: Email)

@Serializable data class UserRegistrationResponse(val userId: UserId)

@Serializable
data class UserLoginRequest(val username: Username, val password: Password, val deviceId: DeviceId)

@Serializable
data class UserLoginResponse(
    val refreshToken: RefreshToken,
    val accessToken: AccessToken,
    val user: User,
)
