package org.castlebet.chess.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.WithAssertions
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Page
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
internal class MongoPlayersTest : WithAssertions {

    companion object {
        private lateinit var mongodbContainer: MongoDBContainer
        private lateinit var client: CoroutineClient
        lateinit var playersCollection: PlayerCollection


        @BeforeAll()
        @JvmStatic
        fun setupClient() {
            @Container
            mongodbContainer = MongoDBContainer("mongo:4.4.6-bionic")
            mongodbContainer.start()

            val address = mongodbContainer.host
            val port = mongodbContainer.firstMappedPort

            client = KMongo.createClient("mongodb://$address:$port").coroutine
            playersCollection = client.toPlayerCollection()
        }

    }

    private val mongoPlayers = MongoPlayers(playersCollection)

    @BeforeEach
    fun setup() {
        runBlocking {
            mongoPlayers.clear()
        }
    }

    @Test
    fun `add should prevent adding player with a nickname already used`() {
        runBlocking {
            mongoPlayers.add(PlayerToCreate(Nickname("superman"), PlayerId("1")))
            assertThatThrownBy {
                runBlocking {
                    mongoPlayers.add(PlayerToCreate(Nickname("superman"), PlayerId("2")))
                }
            }.hasMessage("Nickname superman already exists")
        }
    }


    @Test
    fun `get all should return all players`() {
        runBlocking {
            assertThat(mongoPlayers.getAll().players).isEmpty()
            mongoPlayers.add(PlayerToCreate(Nickname("superman"), PlayerId("1")))
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            val result = mongoPlayers.getAll(Page("1"))
            assertThat(result.players.size).isEqualTo(2)
            assertThat(result.players.first()).isEqualTo(PlayerResult(PlayerId("1"), Nickname("superman"), Score(0)))
        }
    }

    @Test
    fun `get should return null when player not found`() {
        runBlocking {
            assertThat(mongoPlayers.get(PlayerId("unknown"))).isNull()
        }
    }

    @Test
    fun `update should update score`() {
        runBlocking {
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            mongoPlayers.update(PlayerToUpdate(PlayerId("2"), Score(10)))
            val result = mongoPlayers.get(PlayerId("2"))
            assertThat(result).isEqualTo(PlayerResult(PlayerId("2"), Nickname("batman"), Score(10)))
        }
    }

    @Test
    fun `update should return failure when player does not exist`() {
        runBlocking {
            val result = mongoPlayers.update(PlayerToUpdate(PlayerId("2"), Score(10)))
            assertThat(result).isInstanceOf(UpdatePlayerResult.NotFound::class.java)
        }
    }

    @Test
    fun `clear should remove all players`() {
        runBlocking {
            assertThat(mongoPlayers.getAll().players).isEmpty()
            mongoPlayers.add(PlayerToCreate(Nickname("superman"), PlayerId("1")))
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            val result = mongoPlayers.getAll()
            assertThat(result.players.size).isEqualTo(2)
            mongoPlayers.clear()
            assertThat(mongoPlayers.getAll().players).isEmpty()
        }
    }

}
