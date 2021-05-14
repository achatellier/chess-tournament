package org.castlebet.chess.infrastructure.persistence

import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult

class MongoPlayers : Players {

    private val players = mutableMapOf<PlayerId, PlayerDb>()

    override fun add(player: PlayerToCreate) {
        players[player.playerId] = player.toDb()
    }

    override fun get(id: PlayerId) = players[id]?.toPlayerResult()

    override fun update(player: PlayerToUpdate) =
        players[player.id]
            ?.copy(score = player.score.value)
            ?.also { players[player.id] = it }
            ?.let { UpdatePlayerResult.Success }
            ?: UpdatePlayerResult.NotFound

    override fun getAll() = players.values.map { it.toPlayerResult() }

    override fun clear() = players.clear()

    data class PlayerDb(val _id: String, val nickname: String, val score: Int = 0) {
        fun toPlayerResult() = PlayerResult(PlayerId(_id), nickname, Score(score))
    }

    private fun PlayerToCreate.toDb() = PlayerDb(playerId.value, nickname)
}