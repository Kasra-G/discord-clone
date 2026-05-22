package com.ghkasra.discordclone

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*

fun Application.configureCallLogging() {
  install(CallLogging)
}
