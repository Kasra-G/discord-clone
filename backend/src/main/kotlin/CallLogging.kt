package com.ghkasra.discordclone

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureCallLogging() {
  install(CallLogging) {
    filter { call ->
      call.request.path() != "/api/health"
    }
  }
  val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  install(MicrometerMetrics) {
    registry = appMicrometerRegistry
  }
  routing {
    get("/metrics") {
      call.respond(appMicrometerRegistry.scrape())
    }
  }
}
