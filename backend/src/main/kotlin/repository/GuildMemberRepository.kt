package com.ghkasra.discordclone.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class GuildMember(
    val guildId: GuildId,
    val user: User,
    val updatedAt: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now(),
)

object GuildMembers : CompositeIdTable("guild_members") {
  val userId = reference("user_id", Users.id, ReferenceOption.RESTRICT)
  val guildId = reference("guild_id", Guilds.id, ReferenceOption.CASCADE)
  val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
  val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }

  init {
    addIdColumn(guildId)
    addIdColumn(userId)
  }
}

class GuildMemberRepository(val db: Database) {

  init {
    transaction(db) { SchemaUtils.create(GuildMembers) }
  }

  @WithSpan
  fun listGuildMembers(guildId: GuildId): List<GuildMember> =
      transaction(db) {
        (GuildMembers.innerJoin(Users))
            .selectAll()
            .where { GuildMembers.guildId.eq(guildId.value) }
            .map { it.toGuildMember() }
            .toList()
      }

  @WithSpan
  fun listUserGuilds(userId: UserId): List<Guild> =
      transaction(db) {
        (GuildMembers.innerJoin(Guilds))
            .selectAll()
            .where { GuildMembers.userId eq userId.value }
            .map { it.toGuild() }
            .toList()
      }

  @WithSpan
  fun get(guildId: GuildId, userId: UserId): GuildMember =
      transaction(db) {
        (GuildMembers.innerJoin(Users))
            .selectAll()
            .where {
              GuildMembers.id eq
                  CompositeID {
                    it[GuildMembers.guildId] = guildId.value
                    it[GuildMembers.userId] = userId.value
                  }
            }
            .single()
            .toGuildMember()
      }

  @WithSpan
  fun delete(guildId: GuildId, userId: UserId): Unit =
      transaction(db) {
        GuildMembers.deleteReturning {
              GuildMembers.id eq
                  CompositeID {
                    it[GuildMembers.guildId] = guildId.value
                    it[GuildMembers.userId] = userId.value
                  }
            }
            .single()
      }

  @WithSpan
  fun create(guildId: GuildId, userId: UserId): Unit =
      transaction(db) {
        GuildMembers.insertReturning {
              it[this.guildId] = guildId.value
              it[this.userId] = userId.value
            }
            .single()
      }
}

fun ResultRow.toGuildMember() =
    GuildMember(
        guildId = GuildId(get(GuildMembers.guildId).value),
        user = toUser(),
        createdAt = get(GuildMembers.createdAt),
        updatedAt = get(GuildMembers.updatedAt),
    )
