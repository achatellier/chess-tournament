package org.castlebet.chess.domain

interface Players {

    suspend fun add(player: PlayerToCreate): PlayerToCreate

    suspend fun get(id: PlayerId): PlayerResult?

    suspend fun get(nickname: Nickname): PlayerResult?

    suspend fun update(player: PlayerToUpdate): UpdatePlayerResult

    suspend fun getAll(page: Page? = null): GetPlayerResult

    suspend fun clear()
}
