package org.castlebet.chess.domain

interface Players {

    suspend fun add(player: PlayerToCreate): CreatedPlayerResult

    suspend fun update(player: PlayerToUpdate): UpdatePlayerResult

    suspend fun clear()
}
