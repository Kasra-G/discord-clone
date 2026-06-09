package com.ghkasra.discordclone.model

import io.ktor.http.HttpStatusCode

sealed class ServiceException(
    val status: HttpStatusCode,
    override val message: String,
    override val cause: Throwable?,
) : Exception(message, cause) {
  data class Unauthorized(override val message: String, override val cause: Throwable? = null) :
      ServiceException(HttpStatusCode.Unauthorized, message, cause)

  data class NotFound(override val message: String, override val cause: Throwable? = null) :
      ServiceException(HttpStatusCode.NotFound, message, cause)
}
