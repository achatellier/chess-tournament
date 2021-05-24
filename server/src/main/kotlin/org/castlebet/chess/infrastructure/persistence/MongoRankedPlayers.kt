package org.castlebet.chess.infrastructure.persistence

import com.mongodb.client.model.Filters.elemMatch
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Projections
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
                TournamentRanksDb(DEFAULT_TOURNAMENT_ID, request.rankedPlayers.map { it.toDb() }, request.transactionId, request.rankedPlayers.size),
                ReplaceOptions().upsert(request.upsertNeeded())
            )
    }

    override suspend fun get(id: PlayerId) =
        playerRanks.find(
            elemMatch(Keys.PLAYERS, eq(Keys.ID, id.value))
        ).projection(
            Projections.fields(
                Projections.elemMatch(Keys.PLAYERS, eq(Keys.ID, id.value)),
                eq(Keys.TOURNAMENT_ID, 1),
                eq(Keys.TRANSACTION_ID, 1),
            )
        )
            .first()
            ?.players
            ?.first()
            ?.toRanksResult()


    override suspend fun clear() {
        playerRanks.deleteMany(EMPTY_BSON)
    }

    override suspend fun getAll(page: Page) = playerRanks
        .find(eq(Keys.TOURNAMENT_ID, DEFAULT_TOURNAMENT_ID))
        .projection(
            Projections.fields(
                Projections.slice(Keys.PLAYERS, (page.value - 1) * page.pageSize, page.pageSize),
                eq(Keys.COUNT, 1),
                eq(Keys.TOURNAMENT_ID, 1),
                eq(Keys.TRANSACTION_ID, 1),
            )
        )
        .first()
        ?.toPlayerResult()
        ?: RankedPlayersResult(0, emptyList())


    data class PlayerRankDb(val _id: String, val nickname: String, val score: Int?, val rank: Int?) {
        fun toRanksResult() = RankedPlayer(PlayerId(_id), Nickname(nickname), score?.let { Score(it) }, rank?.let { Rank(it) })
    }

    private fun RankedPlayer.toDb() = PlayerRankDb(id.value, nickname.value, score?.value, rank?.value)
    data class TournamentRanksDb(val tournamentId: String, val players: List<PlayerRankDb>?, val transactionId: Int, val count: Int) {
        fun toPlayerResult() = RankedPlayersResult(count, players?.map { it.toRanksResult() } ?: emptyList())
    }

}
