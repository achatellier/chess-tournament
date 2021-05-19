package org.castlebet.chess.domain

import java.util.UUID

data class PlayerToCreate(val nickname: String, val playerId: PlayerId = PlayerId()) {
    init {
        require(nickname.isNotBlank()) { "Nickname must not be empty" }
    }
}

data class PlayerToUpdate(val id: PlayerId, val score: Score)
data class PlayerResult(val id: PlayerId, val nickname: String, val score: Score)
data class Score(val value: Int)
data class PlayerId(val value: String = UUID.randomUUID().toString())
sealed class UpdatePlayerResult {
    object Success : UpdatePlayerResult()
    object NotFound : UpdatePlayerResult()
}
