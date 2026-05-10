package com.example.com

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*

fun Application.configureHttp() {
  install(CORS) {
    anyMethod()
    allowHost(
        System.getenv("DOMAIN") ?: "*"
    ) // @TODO: Don't do this in production if possible. Try to limit it.
  }
}
