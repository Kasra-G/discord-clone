@file:OptIn(ExperimentalUuidApi::class)

package com.ghkasra.discordclone.model

import com.ghkasra.discordclone.service.User
import com.ghkasra.discordclone.service.UserId
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ChannelId(val value: String) {
  companion object {
    val DEFAULT = ChannelId("default")
  }
}

@Serializable @JvmInline value class MessageId(val value: Uuid)

data class SaveMessageRequest(
    val channelId: ChannelId = ChannelId.DEFAULT,
    val content: String,
    val authorId: UserId,
)

@Serializable
data class Message(
    val channelId: ChannelId,
    val content: String,
    val author: User,
    val id: MessageId,
    val createdAt: Instant,
    val updatedAt: Instant,
)
