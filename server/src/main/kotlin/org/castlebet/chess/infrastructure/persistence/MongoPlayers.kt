package org.castlebet.chess.infrastructure.persistence

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.push
import com.mongodb.client.model.Updates.set
import org.castlebet.chess.domain.CreatedPlayerResult
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Player
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection

typealias PlayerCollection = CoroutineCollection<MongoPlayers.TournamentPlayersDb>

fun CoroutineClient.toPlayerCollection(): PlayerCollection = getDatabase("tournament").getCollection("tournament-players")

class Keys {
    companion object {
        const val ID = "_id"
        const val COUNT = "count"
        const val TOURNAMENT_ID = "tournamentId"
        const val TRANSACTION_ID = "transactionId"
        const val SCORE = "score"
        const val PLAYERS = "players"
    }
}

/*
   The tournament concept should appear in the domain and in all APIs.
   To stay simple and focus on the exercise I've made the choice of always having a unique tournament used by default, created on demand
 */
const val DEFAULT_TOURNAMENT_ID = "my-tournament"

class MongoPlayers(
    private val players: PlayerCollection
) : Players {

    override suspend fun add(player: PlayerToCreate): CreatedPlayerResult {
        require(get(player.nickname) == null) { "Nickname ${player.nickname.value} already exists" }
        return (players.findOneAndUpdate(
            eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID),
            Updates.combine(
                set(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID),
                push(Keys.PLAYERS, player.toDb()),
                //create a document is atomic so this transaction id is unique
                inc(Keys.TRANSACTION_ID, 1)
            ),
            FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        )?.let {
            CreatedPlayerResult(
                it.transactionId,
                it.players.first { it._id == player.id.value }.toPlayer(),
                it.players.map { player -> player.toPlayer() })
        } ?: throw IllegalStateException("An error occured during player creation"))
    }

    suspend fun get(predicate: (PlayerDb) -> Boolean) =
        players.findOne(and(eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID)))
            ?.players
            ?.firstOrNull(predicate)

    suspend fun get(id: PlayerId): PlayerDb? = get { it._id == id.value }

    suspend fun get(nickname: Nickname): PlayerDb? = get { it.nickname == nickname.value }

    suspend fun getAll() = (players.findOne(EMPTY_BSON) ?: TournamentPlayersDb(players = listOf()))
        .players
        .map { it.toPlayer() }

    override suspend fun update(player: PlayerToUpdate) =
        players.findOneAndUpdate(
            Updates.combine(
                eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID),
                eq("${Keys.PLAYERS}.${Keys.ID}", player.id.value),
            ),
            Updates.combine(
                set("${Keys.PLAYERS}.$.${Keys.SCORE}", player.score.value),
                //Update on a document is atomic so this transaction id is unique (See README.md for more info on transactionId)
                inc(Keys.TRANSACTION_ID, 1)
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        )
            ?.let { UpdatePlayerResult.Success(it.transactionId, it.players.map { player -> player.toPlayer() }) }
            ?: UpdatePlayerResult.NotFound

    override suspend fun clear() {
        players.deleteMany(EMPTY_BSON)
    }

    data class PlayerDb(val _id: String, val nickname: String, val score: Int? = null) {
        fun toPlayer() = Player(PlayerId(_id), Nickname(nickname), score?.let { Score(it) })
    }

    data class TournamentPlayersDb(val tournamentId: String = DEFAULT_TOURNAMENT_ID, val players: List<PlayerDb>, val transactionId: Int = 0)

    private fun PlayerToCreate.toDb() = PlayerDb(id.value, nickname.value)

}
