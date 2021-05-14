package org.castlebet.chess.domain

import org.castlebet.chess.infrastructure.persistence.MongoPlayers

interface Players {

    suspend fun add(player: PlayerToCreate)

    suspend fun get(id: PlayerId): PlayerResult?

    suspend fun update(player: PlayerToUpdate): MongoPlayers.UpdatePlayerResult

    suspend fun getAll(): List<PlayerResult>

    suspend fun clear()
}