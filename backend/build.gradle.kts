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

dependencies {
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(ktorLibs.server.websockets)
  implementation(ktorLibs.server.requestValidation)
  implementation(libs.logback.classic)
  implementation(ktorLibs.server.callLogging)
  implementation(libs.exposed.core)
  implementation(libs.exposed.dao)
  implementation(libs.exposed.jdbc)
  implementation(libs.exposed.kotlin.datetime)
  //  implementation(libs.exposed.migration.core)
  //  implementation(libs.exposed.migration.jdbc)

  implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
  implementation("org.xerial:sqlite-jdbc:3.53.1.0")

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}

tasks.register("format") { dependsOn("ktfmtFormat") }
