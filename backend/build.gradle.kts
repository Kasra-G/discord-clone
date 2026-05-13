plugins {
  alias(libs.plugins.kotlin.jvm)
  kotlin("plugin.serialization") version "2.3.20"
  alias(ktorLibs.plugins.ktor)
  id("com.ncorti.ktfmt.gradle") version "0.26.0"
}

group = "com.example.com"

version = "1.0.0-SNAPSHOT"

application { mainClass = "io.ktor.server.netty.EngineMain" }

kotlin { jvmToolchain(25) }

dependencies {
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(ktorLibs.server.websockets)
  implementation(libs.logback.classic)
  implementation(ktorLibs.server.callLogging)

  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}

tasks.register("format") { dependsOn("ktfmtFormat") }
