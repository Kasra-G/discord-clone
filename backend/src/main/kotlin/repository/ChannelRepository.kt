package com.ghkasra.discordclone.repository

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
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
import org.jetbrains.exposed.v1.jdbc.updateReturning

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
  val guildId = reference("guild_id", Guilds.id, ReferenceOption.CASCADE)
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

  fun list(guildId: GuildId): List<Channel> =
      transaction(db) {
        Channels.selectAll()
            .where { Channels.guildId.eq(guildId.value) }
            .map { it.toChannel() }
            .toList()
      }

  fun get(channelId: ChannelId): Channel =
      transaction(db) {
        Channels.selectAll().where { Channels.id.eq(channelId.value) }.single().toChannel()
      }

  fun update(channelId: ChannelId, name: String?, description: String?): Channel =
      transaction(db) {
        Channels.updateReturning(where = { Channels.id.eq(channelId.value) }) {
              name?.let { name -> it[this.name] = name }
              description?.let { description -> it[this.description] = description }
              it[updatedAt] = Clock.System.now()
            }
            .single()
            .toChannel()
      }

  fun delete(channelId: ChannelId): Channel =
      transaction(db) {
        Channels.deleteReturning { Channels.id.eq(channelId.value) }.single().toChannel()
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
        guildId = GuildId(get(Channels.guildId).value),
    )

@Serializable @JvmInline value class ChannelId(val value: Uuid) {}
