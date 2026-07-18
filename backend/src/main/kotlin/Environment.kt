package com.ghkasra.discordclone

fun getEnv(env: String): String? = System.getenv(env)

fun getEnvOrThrow(env: String): String =
    getEnv(env) ?: throw IllegalArgumentException("No environment variable $env")

object Environment {
  val JWT_SECRET by lazy { getEnvOrThrow("JWT_SECRET") }
  val DOMAIN by lazy { getEnv("DOMAIN") ?: "*" }
  val DATABASE_URL by lazy { getEnvOrThrow("DATABASE_URL") }
  val HOST by lazy { getEnvOrThrow("HOST") }
}
