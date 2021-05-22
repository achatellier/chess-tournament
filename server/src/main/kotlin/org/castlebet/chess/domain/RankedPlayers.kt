package org.castlebet.chess.domain

interface RankedPlayers {

    suspend fun get(id: PlayerId): RankedPlayer?

    suspend fun getAll(page: Page  = Page()): RankedPlayersResult

    suspend fun clear()

    suspend fun update(request: UpdateRanksRequest)
}
