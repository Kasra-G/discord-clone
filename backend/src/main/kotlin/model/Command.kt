package com.ghkasra.discordclone.model

import com.ghkasra.discordclone.repository.Message
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

@Serializable
sealed class ClientCommand<out T>(@Serializable val command: String) {
  @Serializable val timestamp: Instant = Clock.System.now()
  @Serializable abstract val payload: T

  @Serializable
  data class NewMessage(override val payload: Message) :
      ClientCommand<Message>(command = "NEW_MESSAGE") {}
}
