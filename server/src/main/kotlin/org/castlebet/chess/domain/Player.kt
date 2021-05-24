package org.castlebet.chess.domain

import java.util.UUID
import kotlin.math.min

data class PlayerToCreate(val nickname: Nickname, val id: PlayerId = PlayerId())
data class PlayerToUpdate(val id: PlayerId, val score: ScoreToUpdate)

data class CreatedPlayerResult(val transactionId: Int, val createdPlayer: Player, val players: List<Player>)

sealed class UpdatePlayerResult {
    data class Success(val transactionId: Int, val players: List<Player>) : UpdatePlayerResult()
    object NotFound : UpdatePlayerResult()
}


data class Player(val id: PlayerId, val nickname: Nickname, val score: Score?)

data class PlayerId(val value: String = UUID.randomUUID().toString())
data class Nickname(val value: String) : Comparable<Nickname> {
    init {
        require(value.isNotBlank()) { "Nickname must not be empty" }
    }

    override fun compareTo(other: Nickname) = compareValues(value, other.value)
}

data class ScoreToUpdate(val value: Int) {
    init {
        require(value >= 0) { "A player's score should be a positive number" }
    }
}

data class Score(val value: Int?) : Comparable<Score> {
    override fun compareTo(other: Score) = compareValues(value, other.value)
}

data class Rank(val value: Int)

class Page(stringValue: String = "1") {
    init {
        try {
            require(Integer.valueOf(stringValue) >= 1)
        } catch (e: Exception) {
            throw IllegalArgumentException("A page value should be a number >= 1")
        }
    }

    val value: Int = Integer.valueOf(stringValue)
    val pageSize = 30
    fun toStartIndex() = (value - 1) * pageSize
    fun toEndIndex() = pageSize * value - 1
}
