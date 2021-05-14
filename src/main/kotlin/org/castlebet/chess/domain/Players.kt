package org.castlebet.chess.domain

interface Players {

    fun add(player: PlayerToCreate)

    fun get(id: PlayerId): PlayerResult?

    fun update(player: PlayerToUpdate): UpdatePlayerResult

    fun getAll(): List<PlayerResult>

    fun clear()
}