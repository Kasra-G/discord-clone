package com.ghkasra.discordclone

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ghkasra.discordclone.model.ServiceException
import com.ghkasra.discordclone.service.AuthenticationService
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity() {
  val jwtAudience = AuthenticationService.AUDIENCE
  val jwtDomain = AuthenticationService.ISSUER
  val jwtRealm = AuthenticationService.REALM
  val jwtSecret = Environment.JWT_SECRET
  authentication {
    jwt {
      realm = jwtRealm
      authHeader { call ->
        val value =
            call.request.cookies[AuthenticationService.ACCESS_TOKEN_COOKIE_NAME]
                ?: throw ServiceException.Unauthorized("Missing access token authorization cookie")
        HttpAuthHeader.Single("Bearer", value)
      }
      verifier(
          JWT.require(Algorithm.HMAC256(jwtSecret))
              .withAudience(jwtAudience)
              .withIssuer(jwtDomain)
              .build()
      )
      validate { credential ->
        if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload)
        else null
      }
      challenge { defaultScheme, realm ->
        throw ServiceException.Unauthorized("Access token is invalid or expired")
      }
    }
  }
}
