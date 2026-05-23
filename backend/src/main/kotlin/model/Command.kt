package com.ghkasra.discordclone.model

import com.ghkasra.discordclone.service.Username
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("command")
sealed class ServerCommand {

  @Serializable
  @SerialName("BROADCAST_MESSAGE")
  data class BroadcastMessage(val message: String) : ServerCommand() {}

  @Serializable
  @SerialName("PRIVATE_MESSAGE")
  data class PrivateMessage(val message: String, val recipient: String) : ServerCommand() {}
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("command")
sealed class ClientCommand {
  val timestamp: Instant = Clock.System.now()

  @Serializable
  @SerialName("NEW_MESSAGE")
  data class NewMessage(val message: String, val sender: Username) : ClientCommand() {}
}
