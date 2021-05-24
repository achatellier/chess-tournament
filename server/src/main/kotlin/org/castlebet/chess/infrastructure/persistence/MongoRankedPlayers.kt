package org.castlebet.chess.infrastructure.persistence

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Page
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.Rank
import org.castlebet.chess.domain.RankedPlayer
import org.castlebet.chess.domain.RankedPlayers
import org.castlebet.chess.domain.RankedPlayersResult
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdateRanksRequest
import org.castlebet.chess.domain.subList
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection

typealias RankedPlayerCollection = CoroutineCollection<MongoRankedPlayers.TournamentRanksDb>

fun CoroutineClient.toRankedPlayerCollection(): RankedPlayerCollection = getDatabase("tournament").getCollection("tournament-ranks")

class MongoRankedPlayers(
    private val playerRanks: RankedPlayerCollection
) : RankedPlayers {

    override suspend fun update(request: UpdateRanksRequest) {
        playerRanks
            .replaceOne(
                if (request.upsertNeeded()) {
                    eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID)
                } else {
                    Updates.combine(
                        eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID),
                        //Only update document if needed, a more recent update may have been made before (see README.md)
                        lt(Keys.TRANSACTION_ID, request.transactionId)
                    )
                },
                TournamentRanksDb(DEFAULT_TOURNAMENT_ID, request.rankedPlayers.map { it.toDb() }, request.transactionId),
                ReplaceOptions().upsert(request.upsertNeeded())
            )
    }

    suspend fun get(predicate: (PlayerRankDb) -> Boolean) =
        playerRanks.findOne(eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID))
            ?.players?.also { println(it.toString()) }
            ?.firstOrNull(predicate)
            ?.toRanksResult()


    override suspend fun get(id: PlayerId): RankedPlayer? = get { it._id == id.value }

    override suspend fun clear() {
        playerRanks.deleteMany(EMPTY_BSON)
    }

    override suspend fun getAll(page: Page): RankedPlayersResult {
        val players = playerRanks.findOne(eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID))?.toPlayerResult()
        return RankedPlayersResult(
            players?.size ?: 0,
            players.subList(page)
        )
    }


    data class PlayerRankDb(val _id: String, val nickname: String, val score: Int?, val rank: Int?) {
        fun toRanksResult() = RankedPlayer(PlayerId(_id), Nickname(nickname), score?.let { Score(it) }, rank?.let { Rank(it) })
    }

    private fun RankedPlayer.toDb() = PlayerRankDb(id.value, nickname.value, score?.value, rank?.value)
    data class TournamentRanksDb(val tournamentId: String, val players: List<PlayerRankDb>, val transactionId: Int) {
        fun toPlayerResult() = players.map { it.toRanksResult() }
    }

}
