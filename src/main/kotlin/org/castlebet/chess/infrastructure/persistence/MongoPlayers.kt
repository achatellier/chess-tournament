package org.castlebet.chess.infrastructure.persistence

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import com.mongodb.client.result.UpdateResult
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.infrastructure.persistence.MongoPlayers.UpdatePlayerResult.NotFound
import org.castlebet.chess.infrastructure.persistence.MongoPlayers.UpdatePlayerResult.Success
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.json
import org.litote.kmongo.util.idValue

typealias PlayerCollection = CoroutineCollection<MongoPlayers.PlayerDb>

fun CoroutineClient.toPlayerCollection(): PlayerCollection = getDatabase("tournament").getCollection("tournament-players")

class MongoPlayers(private val players: PlayerCollection) : Players {

        //TODO handle players with same nickname
    override suspend fun add(player: PlayerToCreate): PlayerToCreate =            player.also { players.insertOne(player.toDb()) }

    override suspend fun get(id: PlayerId) = players.findOne(eq("_id", id.value))?.toPlayerResult()

    override suspend fun update(player: PlayerToUpdate) =
        players.updateOne(eq("_id", player.id.value), set("score", player.score.value)).toResult()

    private fun UpdateResult.toResult() = when (matchedCount) {
        1L -> Success
        0L -> NotFound
        else -> throw IllegalStateException("Non unique id found in database for the document ${this.json}") //no need to pollute the rest of the app with this
    }

    sealed class UpdatePlayerResult {
        object Success : UpdatePlayerResult()
        object NotFound : UpdatePlayerResult()
    }

    override suspend fun clear() {
        players.deleteMany(EMPTY_BSON)
    }

    override suspend fun getAll() = players.find().toList().map { it.toPlayerResult() }

    data class PlayerDb(val _id: String, val nickname: String, val score: Int = 0) {
        fun toPlayerResult() = PlayerResult(PlayerId(_id), nickname, Score(score))
    }


    private fun PlayerToCreate.toDb() = PlayerDb(playerId.value, nickname)
}