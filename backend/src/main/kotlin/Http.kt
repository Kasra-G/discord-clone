package com.ghkasra.discordclone

import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.configureHttp() {
  install(XForwardedHeaders)
  install(CORS) {
    anyMethod()
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Authorization)
    allowNonSimpleContentTypes = true
    allowHost(
        host = Environment.HOST,
        schemes = listOf("http", "https"),
    ) // @TODO: Don't do this in production if possible. Try to limit it.
  }
}
