package com.ghkasra.discordclone.repository

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable @JvmInline value class GuildId(val value: Uuid)

@Serializable
data class Channel(
    val id: ChannelId,
    val guildId: GuildId,
    val name: String,
    val description: String,
    val updatedAt: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now(),
)

object Channels : UuidTable("channels", uuidVersion = UuidVersion.V7) {
  val guildId = uuid("guild_id")
  val name = text("name")
  val description = text("description")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }

  init {
    uniqueIndex(guildId, id)
  }
}

class ChannelRepository(val db: Database) {

  init {
    transaction(db) { SchemaUtils.create(Channels) }
  }

  fun listChannels(guildId: GuildId): List<Channel> =
      transaction(db) {
        Channels.selectAll()
            .where { Channels.guildId.eq(guildId.value) }
            .map { it.toChannel() }
            .toList()
      }

  fun getChannel(guildId: GuildId, channelId: ChannelId): Channel =
      transaction(db) {
        Channels.selectAll()
            .where { Channels.guildId.eq(guildId.value).and { Channels.id.eq(channelId.value) } }
            .single()
            .toChannel()
      }

  fun deleteChannel(guildId: GuildId, channelId: ChannelId): Channel =
      transaction(db) {
        Channels.deleteReturning {
              Channels.guildId.eq(guildId.value).and { Channels.id.eq(channelId.value) }
            }
            .single()
            .toChannel()
      }

  fun create(guildId: GuildId, name: String, description: String): Channel =
      transaction(db) {
        Channels.insertReturning {
              it[this.guildId] = guildId.value
              it[this.name] = name
              it[this.description] = description
            }
            .single()
            .toChannel()
      }
}

fun ResultRow.toChannel() =
    Channel(
        id = ChannelId(get(Channels.id).value),
        createdAt = get(Channels.createdAt),
        updatedAt = get(Channels.updatedAt),
        name = get(Channels.name),
        description = get(Channels.description),
        guildId = GuildId(get(Channels.guildId)),
    )

@Serializable @JvmInline value class ChannelId(val value: Uuid) {}
