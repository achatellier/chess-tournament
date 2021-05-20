package org.castlebet.chess.domain

import java.util.UUID

data class PlayerToCreate(val nickname: Nickname, val playerId: PlayerId = PlayerId())
data class PlayerToUpdate(val id: PlayerId, val score: Score)

data class GetPlayerResult(val count: Int, val players: List<PlayerResult>)
data class PlayerResult(val id: PlayerId, val nickname: Nickname, val score: Score) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlayerResult
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class PlayerId(val value: String = UUID.randomUUID().toString())
data class Nickname(val value: String) {
    init {
        require(value.isNotBlank()) { "Nickname must not be empty" }
    }
}
data class Score(val value: Int) {
    init {
        require(value >= 0) { "A player's score should be a positive number" }
    }
}

class Page(stringValue: String) {
    val value: Int = Integer.valueOf(stringValue)

    init {
        require(value >= 1) { "A page value should be a number >= 1" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Page
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        return value
    }

}


sealed class UpdatePlayerResult {
    object Success : UpdatePlayerResult()
    object NotFound : UpdatePlayerResult()
}
