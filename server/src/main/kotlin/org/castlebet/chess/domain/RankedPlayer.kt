package org.castlebet.chess.domain

data class RankedPlayersResult(val count: Int, val rankedPlayers: List<RankedPlayer>)
data class RankedPlayer(val id: PlayerId, val nickname: Nickname, val score: Score?, val rank: Rank?, val index: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RankedPlayer
        if (id != other.id) return false
        return true
    }

    override fun hashCode() = id.hashCode()
}

fun Player.toRankedPlayer(upperPlayer: RankedPlayer?, allUpperPlayers: List<RankedPlayer>) = RankedPlayer(id, nickname, score, computeRank(upperPlayer, allUpperPlayers), upperPlayer?.index?.plus(1) ?: 0)
private fun Player.computeRank(upperPlayer: RankedPlayer?, allUpperPlayers: List<RankedPlayer>) = when {
    score == null -> null
    upperPlayer == null -> Rank(1)
    upperPlayer.score == null -> null
    upperPlayer.rank == null -> null
    upperPlayer.score == score -> upperPlayer.rank
    else -> Rank(allUpperPlayers.size + 1)
}

data class UpdateRanksRequest(val transactionId: Int, val rankedPlayers: List<RankedPlayer>) {
    //upsert needed only at the init of the tournament, when the first player is created
    fun upsertNeeded() = rankedPlayers.size <= 1
}


fun List<Player>.toRanked(): List<RankedPlayer> {
    var upperPlayer: RankedPlayer? = null
    val upperPlayers: MutableList<RankedPlayer> = mutableListOf()
    return sortedWith(compareByDescending<Player> { it.score }.thenBy { it.nickname })
        .map { it.toRankedPlayer(upperPlayer, upperPlayers).also { res -> upperPlayer = res; upperPlayers.add(res) } }
}

fun CreatedPlayerResult.toRanked() = UpdateRanksRequest(transactionId, players.toRanked())
fun UpdatePlayerResult.Success.toRanked() = UpdateRanksRequest(transactionId, players.toRanked())
