package com.ghkasra.discordclone

import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv()

fun getEnv(env: String): String? = dotenv[env]

fun getEnvOrThrow(env: String): String =
    dotenv[env] ?: throw IllegalArgumentException("No environment variable $env")
