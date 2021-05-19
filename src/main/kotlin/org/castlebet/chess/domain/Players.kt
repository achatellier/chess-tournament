package org.castlebet.chess.domain

interface Players {

    suspend fun add(player: PlayerToCreate): PlayerToCreate

    suspend fun get(id: PlayerId): PlayerResult?

    suspend fun update(player: PlayerToUpdate): UpdatePlayerResult

    suspend fun getAll(): List<PlayerResult>

    suspend fun clear()
}