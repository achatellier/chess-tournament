package org.castlebet.chess.infrastructure.persistence

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import com.mongodb.client.result.UpdateResult
import org.castlebet.chess.domain.GetPlayerResult
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Page
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.json

typealias PlayerCollection = CoroutineCollection<MongoPlayers.PlayerDb>

fun CoroutineClient.toPlayerCollection(): PlayerCollection = getDatabase("tournament").getCollection("tournament-players")

const val PAGE_SIZE = 30

class Keys {
    companion object {
        const val ID = "_id"
        const val NICK_NAME = "nickname"
        const val SCORE = "score"

    }

}

class MongoPlayers(private val players: PlayerCollection) : Players {

    override suspend fun add(player: PlayerToCreate) = player
        // this check should be done with a unique index but it would require some extended time to do that
        .also { require(get(player.nickname) == null) { "Nickname ${player.nickname.value} already exists" } }
        .also { players.insertOne(player.toDb()) }

    override suspend fun get(id: PlayerId): PlayerResult? {
        return players.findOne(eq(Keys.ID, id.value))?.toPlayerResult()
    }

    override suspend fun get(nickname: Nickname): PlayerResult? {
        return players.findOne(eq(Keys.NICK_NAME, nickname.value))?.toPlayerResult()
    }

    override suspend fun update(player: PlayerToUpdate) =
        players.updateOne(eq(Keys.ID, player.id.value), set(Keys.SCORE, player.score.value)).toResult()

    private fun UpdateResult.toResult() = when (matchedCount) {
        1L -> UpdatePlayerResult.Success
        0L -> UpdatePlayerResult.NotFound
        else -> throw IllegalStateException("Non unique id found in database for the document ${this.json}") //no need to pollute the rest of the app with this
    }

    override suspend fun clear() {
        players.deleteMany(EMPTY_BSON)
    }

    override suspend fun getAll(page: Page?) = GetPlayerResult(
        players.countDocuments(EMPTY_BSON).toInt(),
        players
            .find()
            .limit(PAGE_SIZE)
            .skip(page?.value?.minus(1)?.times(PAGE_SIZE) ?: 0)
            .toList()
            .map { it.toPlayerResult() })


    data class PlayerDb(val _id: String, val nickname: String, val score: Int = 0) {
        fun toPlayerResult() = PlayerResult(PlayerId(_id), Nickname(nickname), Score(score))
    }

    private fun PlayerToCreate.toDb() = PlayerDb(playerId.value, nickname.value)

}
