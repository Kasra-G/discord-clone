package com.ghkasra.discordclone.service

import com.ghkasra.discordclone.model.ServiceException
import com.ghkasra.discordclone.repository.DeviceId
import com.ghkasra.discordclone.repository.Email
import com.ghkasra.discordclone.repository.User
import com.ghkasra.discordclone.repository.UserId
import com.ghkasra.discordclone.repository.UserRepository
import kotlinx.serialization.Serializable

class UserService(val authService: AuthenticationService, val userRepository: UserRepository) {

  fun register(request: UserRegistrationRequest): UserRegistrationResponse {
    val isUsernameAvailable = userRepository.findByUsername(request.username) == null

    require(isUsernameAvailable) { "Username ${request.username} is not available" }

    val userDetails =
        userRepository.create(
            username = request.username,
            email = request.email,
        )
    authService.storeUserLoginCredentials(UserLogin(request.username, request.password))

    return UserRegistrationResponse(userDetails.id)
  }

  fun login(request: UserLoginRequest): UserLoginResponse {
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
