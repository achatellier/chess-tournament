package org.castlebet.chess.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.WithAssertions
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.ScoreToUpdate
import org.castlebet.chess.domain.UpdatePlayerResult
import org.castlebet.chess.infrastructure.persistence.MongoPlayers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertTrue


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
    fun `add should add player`() {
        runBlocking {
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            val result = mongoPlayers.get(PlayerId("2"))
            assertThat(result).isEqualTo(PlayerDb("2", "batman", null))
        }
    }


    @Test
    fun `update should update score`() {
        runBlocking {
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            val updatePlayerResult = mongoPlayers.update(PlayerToUpdate(PlayerId("2"), ScoreToUpdate(10)))
            assertTrue(updatePlayerResult is UpdatePlayerResult.Success)
            assertThat(updatePlayerResult.players.first().score?.value).isEqualTo(10)
            val result = mongoPlayers.get(PlayerId("2"))
            assertThat(result).isEqualTo(PlayerDb("2", "batman", 10))
        }
    }

    @Test
    fun `update should return failure when player does not exist`() {
        runBlocking {
            val result = mongoPlayers.update(PlayerToUpdate(PlayerId("2"), ScoreToUpdate(10)))
            assertThat(result).isInstanceOf(UpdatePlayerResult.NotFound::class.java)
        }
    }

    @Test
    fun `clear should remove all players`() {
        runBlocking {
            assertThat(mongoPlayers.getAll()).isEmpty()
            mongoPlayers.add(PlayerToCreate(Nickname("superman"), PlayerId("1")))
            mongoPlayers.add(PlayerToCreate(Nickname("batman"), PlayerId("2")))
            val result = mongoPlayers.getAll()
            assertThat(result.size).isEqualTo(2)
            mongoPlayers.clear()
            assertThat(mongoPlayers.getAll()).isEmpty()
        }
    }

}
