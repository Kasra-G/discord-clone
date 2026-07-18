import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.klahap.dotenv.DotEnvBuilder

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
  id("com.ncorti.ktfmt.gradle") version "0.26.0"
  id("io.github.klahap.dotenv") version "1.1.3"
}

group = "com.ghkasra.discordclone"

version = "1.0.0-SNAPSHOT"

application { mainClass = "io.ktor.server.netty.EngineMain" }

kotlin { jvmToolchain(25) }

val otelAgent by configurations.registering {
  isCanBeConsumed = false
  isCanBeResolved = true
}

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
  implementation("io.opentelemetry:opentelemetry-extension-kotlin")
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

  implementation("at.favre.lib:bcrypt:0.10.2") // bcrypt
  implementation("org.xerial:sqlite-jdbc:3.53.1.0") // sqlite

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}

tasks.named<JavaExec>("run") {
  doFirst {
    val otelAgentJar = configurations[otelAgent.name].singleFile.absolutePath
    jvmArgs("-javaagent:$otelAgentJar")

    val envVars = DotEnvBuilder.dotEnv {
      addFile("$rootDir/.env")
    }
    listOf(
            "DOMAIN",
            "HOST",
            "DATABASE_URL",
            "JWT_SECRET",
        )
        .forEach {
          environment(
              it,
              envVars[it] ?: throw IllegalArgumentException("Required env variable $it not set."),
          )
        }
  }
  environment("OTEL_SERVICE_NAME", "ktor-dev")
  environment("OTEL_TRACES_EXPORTER", "otlp")
  environment("OTEL_METRICS_EXPORTER", "otlp")
  environment("OTEL_LOGS_EXPORTER", "otlp")
  environment("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
  environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")
  systemProperty("jansi.passthrough", "true")
}

tasks.register("format") { dependsOn("ktfmtFormat") }

tasks.withType<ShadowJar> {
  archiveClassifier.set("all")

  exclude("org/sqlite/native/Windows/**")
  exclude("org/sqlite/native/Mac/**")
  exclude("org/sqlite/native/FreeBSD/**")
  exclude("org/sqlite/native/Linux/arm/**")
  exclude("org/sqlite/native/Linux/armv6/**")
  exclude("org/sqlite/native/Linux/aarch64/**")
  exclude("org/sqlite/native/Linux/android/**")
  exclude("org/sqlite/native/Linux/ppc64le/**")
  exclude("org/sqlite/native/Linux/riscv64/**")
  exclude("org/sqlite/native/Linux/s390x/**")
  // exclude("org/sqlite/native/Linux/x86_64/**") // Keeping this one as an example

  exclude("META-INF/maven/**")
  exclude("META-INF/LICENSE*")
  exclude("META-INF/NOTICE*")
  exclude("META-INF/*.SF")
  exclude("META-INF/*.DSA")
  exclude("META-INF/*.RSA")
  exclude("module-info.class")
}
