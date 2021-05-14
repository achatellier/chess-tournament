package org.castlebet.chess.infrastructure.persistence

import org.assertj.core.api.WithAssertions
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs


internal class MongoPlayersTest : WithAssertions {

    private val mongoPlayers = MongoPlayers()

    //    @BeforeEach
    fun setup() {
        mongoPlayers.clear()
    }

    @Test
    fun `get all should return all players`() {
        assertThat(mongoPlayers.getAll()).isEmpty()
        mongoPlayers.add(PlayerToCreate("superman", PlayerId("1")))
        mongoPlayers.add(PlayerToCreate("batman", PlayerId("2")))
        val result = mongoPlayers.getAll()
        assertThat(result.size).isEqualTo(2)
        assertThat(result.first()).isEqualTo(PlayerResult(PlayerId("1"), "superman", Score(0)))
    }

    @Test
    fun `get should return null when player not found`() {
        assertThat(mongoPlayers.get(PlayerId("unknown"))).isNull()
    }

    @Test
    fun `update should update score`() {
        mongoPlayers.add(PlayerToCreate("batman", PlayerId("2")))
        mongoPlayers.update(PlayerToUpdate(PlayerId("2"), Score(10)))
        val result = mongoPlayers.get(PlayerId("2"))
        assertThat(result).isEqualTo(PlayerResult(PlayerId("2"), "batman", Score(10)))
    }

    @Test
    fun `update should return failure when player does not exist`() {
        val result = mongoPlayers.update(PlayerToUpdate(PlayerId("2"), Score(10)))
        assertIs<UpdatePlayerResult.NotFound>(result)
    }

    @Test
    fun `clear should remove all players`() {
        assertThat(mongoPlayers.getAll()).isEmpty()
        mongoPlayers.add(PlayerToCreate("superman", PlayerId("1")))
        mongoPlayers.add(PlayerToCreate("batman", PlayerId("2")))
        val result = mongoPlayers.getAll()
        assertThat(result.size).isEqualTo(2)
        mongoPlayers.clear()
        assertThat(mongoPlayers.getAll()).isEmpty()
    }

}