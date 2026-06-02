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

  // ktor plugins
  implementation(ktorLibs.server.auth)
  implementation(ktorLibs.server.auth.jwt)
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.statusPages)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.server.websockets)
  implementation(ktorLibs.server.requestValidation)
  implementation(ktorLibs.server.callLogging)
  implementation(ktorLibs.serialization.kotlinx.json)

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

tasks.withType<JavaExec> { systemProperty("jansi.passthrough", "true") }

tasks.register("format") { dependsOn("ktfmtFormat") }
