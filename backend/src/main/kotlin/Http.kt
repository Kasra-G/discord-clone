package com.ghkasra.discordclone

import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHttp() {
  install(CORS) {
    anyMethod()
    allowHeader(HttpHeaders.ContentType)
    allowNonSimpleContentTypes = true
    allowHost(
        host = getEnv("DOMAIN") ?: "*",
        schemes = listOf("http", "https"),
    ) // @TODO: Don't do this in production if possible. Try to limit it.
  }
}
