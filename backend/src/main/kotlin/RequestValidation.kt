package com.ghkasra.discordclone

import com.ghkasra.discordclone.model.ServiceException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.util.logging.error
import kotlinx.serialization.Serializable

@Serializable data class RequestException(val code: Int, val error: String)

fun Application.configureRequestValidation() {
  install(StatusPages) {
    exception<ServiceException> { call, cause ->
      call.respond(
          status = cause.status,
          RequestException(code = cause.status.value, error = cause.message),
      )
    }

    exception<IllegalArgumentException> { call, cause ->
      call.respond(
          status = HttpStatusCode.BadRequest,
          RequestException(
              code = HttpStatusCode.BadRequest.value,
              error = cause.message ?: "Bad Input",
          ),
      )
      call.application.log.error(cause.message)
    }

    exception<Throwable> { call, cause ->
      call.respond(
          status = HttpStatusCode.InternalServerError,
          RequestException(
              code = HttpStatusCode.InternalServerError.value,
              error = cause.message ?: "Unhandled Exception",
          ),
      )
      call.application.log.error(cause)
    }
  }

  install(RequestValidation) {}
}
