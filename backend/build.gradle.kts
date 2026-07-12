plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
  id("com.ncorti.ktfmt.gradle") version "0.26.0"
}

group = "com.ghkasra.discordclone"

version = "1.0.0-SNAPSHOT"

application { mainClass = "io.ktor.server.netty.EngineMain" }

kotlin { jvmToolchain(25) }

val otelAgent: Configuration by configurations.creating

dependencies {

  // ktor plugins
  implementation(ktorLibs.server.auth)
  implementation(ktorLibs.server.auth.jwt)
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.statusPages)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.forwardedHeader)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.server.websockets)
  implementation(ktorLibs.server.requestValidation)
  implementation(ktorLibs.server.callLogging)
  implementation(ktorLibs.server.metrics.micrometer)
  implementation(ktorLibs.serialization.kotlinx.json)

  // telemetry
  implementation(platform("io.opentelemetry:opentelemetry-bom:1.63.0"))
  implementation(platform("io.micrometer:micrometer-bom:1.17.0"))
  implementation(
      platform(
          "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.29.0-alpha"
      )
  )
  otelAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.29.0")

  implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
  runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.micrometer:micrometer-registry-prometheus")

  // logging
  implementation(libs.logback.classic)

  // exposed
  implementation(libs.exposed.core)
  implementation(libs.exposed.dao)
  implementation(libs.exposed.jdbc)
  implementation(libs.exposed.kotlin.datetime)
  //  implementation(libs.exposed.migration.core)
  //  implementation(libs.exposed.migration.jdbc)

  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1") // .env
  implementation("at.favre.lib:bcrypt:0.10.2") // bcrypt
  implementation("org.xerial:sqlite-jdbc:3.53.1.0") // sqlite

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}

tasks.withType<JavaExec> {
  val agentJar = configurations["otelAgent"].incoming.files.singleFile.absolutePath
  doFirst {
    jvmArgs("-javaagent:$agentJar")
  }
  systemProperty("jansi.passthrough", "true")
  environment("OTEL_SERVICE_NAME", "ktor-dev")
  environment("OTEL_TRACES_EXPORTER", "otlp")
  environment("OTEL_METRICS_EXPORTER", "otlp")
  environment("OTEL_LOGS_EXPORTER", "otlp")
  environment("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
  environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")
}

tasks.register("format") { dependsOn("ktfmtFormat") }
