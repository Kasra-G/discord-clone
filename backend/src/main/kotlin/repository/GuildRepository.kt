package com.ghkasra.discordclone.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
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
data class Guild(
    val id: GuildId,
    val ownerId: UserId,
    val name: String,
    val description: String,
    val updatedAt: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now(),
)

object Guilds : UuidTable("guilds", uuidVersion = UuidVersion.V7) {
  val name = text("name")
  val ownerId = reference("owner_id", Users.id, ReferenceOption.RESTRICT)
  val description = text("description")
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }

  init {
    uniqueIndex(ownerId, id)
  }
}

class GuildRepository(val db: Database) {

  init {
    transaction(db) { SchemaUtils.create(Guilds) }
  }

  @WithSpan
  fun list(ownerId: UserId): List<Guild> =
      transaction(db) {
        Guilds.selectAll().where { Guilds.ownerId eq ownerId.value }.map { it.toGuild() }.toList()
      }

  @WithSpan
  fun get(guildId: GuildId): Guild =
      transaction(db) { Guilds.selectAll().where { Guilds.id eq guildId.value }.single().toGuild() }

  @WithSpan
  fun update(guildId: GuildId, name: String?, description: String?): Guild =
      transaction(db) {
        Guilds.updateReturning(where = { Guilds.id eq guildId.value }) {
              name?.let { name -> it[this.name] = name }
              description?.let { description -> it[this.description] = description }
              it[updatedAt] = Clock.System.now()
            }
            .single()
            .toGuild()
      }

  @WithSpan
  fun delete(guildId: GuildId): Guild =
      transaction(db) { Guilds.deleteReturning { Guilds.id eq guildId.value }.single().toGuild() }

  @WithSpan
  fun create(ownerId: UserId, name: String, description: String): Guild =
      transaction(db) {
        Guilds.insertReturning {
              it[this.ownerId] = ownerId.value
              it[this.name] = name
              it[this.description] = description
            }
            .single()
            .toGuild()
      }
}

fun ResultRow.toGuild() =
    Guild(
        id = GuildId(get(Guilds.id).value),
        createdAt = get(Guilds.createdAt),
        updatedAt = get(Guilds.updatedAt),
        name = get(Guilds.name),
        description = get(Guilds.description),
        ownerId = UserId(get(Guilds.ownerId).value),
    )

@Serializable @JvmInline value class GuildId(val value: Uuid) {}
