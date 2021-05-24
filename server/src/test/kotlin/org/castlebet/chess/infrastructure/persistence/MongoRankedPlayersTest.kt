package org.castlebet.chess.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.WithAssertions
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.Rank
import org.castlebet.chess.domain.RankedPlayer
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdateRanksRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
internal class MongoRankedPlayersTest : WithAssertions {

    companion object {
        private lateinit var mongodbContainer: MongoDBContainer
        private lateinit var client: CoroutineClient
        lateinit var playersCollection: RankedPlayerCollection


        @BeforeAll()
        @JvmStatic
        fun setupClient() {
            @Container
            mongodbContainer = MongoDBContainer("mongo:4.4.6-bionic")
            mongodbContainer.start()

            val address = mongodbContainer.host
            val port = mongodbContainer.firstMappedPort

            client = KMongo.createClient("mongodb://$address:$port").coroutine
            playersCollection = client.toRankedPlayerCollection()
        }

    }

    private val mongoPlayers = MongoRankedPlayers(playersCollection)

    @BeforeEach
    fun setup() {
        runBlocking {
            mongoPlayers.clear()
        }
    }

    @Test
    fun `gte should return null for an unknown player`() {
        runBlocking {
            mongoPlayers.update(UpdateRanksRequest(0, listOf(RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0))))
            assertThat(mongoPlayers.get(PlayerId("unknwon"))).isNull()
        }
    }


    @Test
    fun `upsert should add ranked players on an empty database`() {
        runBlocking {
            mongoPlayers.update(UpdateRanksRequest(0, listOf(RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0))))
            val result = mongoPlayers.get(PlayerId("1"))
            assertThat(result?.id).isEqualTo(PlayerId("1"))
            assertThat(result?.nickname).isEqualTo( Nickname("name"))
            assertThat(result?.score).isEqualTo( Score(10))
            assertThat(result?.rank).isEqualTo( Rank(1))
        }
    }

    @Test
    fun `upsert should override ranked players when it exists`() {
        runBlocking {
            mongoPlayers.update(
                UpdateRanksRequest(
                    1,
                    listOf(
                        RankedPlayer(PlayerId("2"), Nickname("name2"), Score(5), Rank(2), 0)
                    )
                )
            )
            mongoPlayers.update(
                UpdateRanksRequest(
                    2,
                    listOf(
                        RankedPlayer(PlayerId("1"), Nickname("name1"), Score(10), Rank(1),0),
                        RankedPlayer(PlayerId("2"), Nickname("name2"), Score(18), Rank(3), 1)
                    )
                )
            )
            val result = mongoPlayers.get(PlayerId("2"))
            assertThat(result?.nickname).isEqualTo(Nickname("name2"))
            assertThat(result?.rank).isEqualTo(Rank(3))
            assertThat(result?.score).isEqualTo(Score(18))

        }
    }

    @ValueSource(ints = [2, 1, 0])
    @ParameterizedTest
    fun `upsert should not override ranked players when a more recent transaction has already been made`(transactionId: Int) {
        runBlocking {
            mongoPlayers.update(
                UpdateRanksRequest(
                    2,
                    listOf(
                        RankedPlayer(PlayerId("2"), Nickname("name"), Score(52), Rank(2), 0)
                    )
                )
            )
            mongoPlayers.update(
                UpdateRanksRequest(
                    transactionId,
                    listOf(
                        RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0),
                        RankedPlayer(PlayerId("2"), Nickname("name"), Score(18), Rank(3), 1)
                    )
                )
            )
            val result = mongoPlayers.get(PlayerId("2"))
            assertThat(result?.nickname).isEqualTo(Nickname("name"))
            assertThat(result?.rank).isEqualTo(Rank(2))
            assertThat(result?.score).isEqualTo(Score(52))
        }
    }


    @Test
    fun `clear should remove all players`() {
        runBlocking {
            assertThat(mongoPlayers.getAll().rankedPlayers).isEmpty()
            mongoPlayers.update(
                UpdateRanksRequest(
                    0,
                    listOf(
                        RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0),
                    )
                )
            )
            val result = mongoPlayers.getAll()
            assertThat(result.count).isEqualTo(1)
            assertThat(result.rankedPlayers.size).isEqualTo(1)
            mongoPlayers.clear()
            assertThat(mongoPlayers.getAll().rankedPlayers).isEmpty()
            assertThat(mongoPlayers.getAll().count).isEqualTo(0)
        }
    }

    @Test
    fun `getAll should handle null page`() {
        runBlocking {
            mongoPlayers.update(
                UpdateRanksRequest(
                    0,
                    listOf(
                        RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0),
                    )
                )
            )
            val result = mongoPlayers.getAll()
            assertThat(result.rankedPlayers.size).isEqualTo(1)
            assertThat(result.rankedPlayers[0]).isEqualTo(RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0))
        }
    }

    @Test
    fun `getAll should handle pagination`() {
        runBlocking {
            mongoPlayers.update(UpdateRanksRequest(0,
                listOf(RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0))
            ))

            mongoPlayers.update(UpdateRanksRequest(1,
                IntRange(1, 31).map { RankedPlayer(PlayerId("" + it), Nickname("name"), Score(10), Rank(1), it - 1) }
            ))
            val result = mongoPlayers.getAll().also { println(it) }
            assertThat(result.rankedPlayers.size).isEqualTo(30)
            assertThat(result.rankedPlayers[0]).isEqualTo(RankedPlayer(PlayerId("1"), Nickname("name"), Score(10), Rank(1), 0))
            assertThat(result.rankedPlayers[1]).isEqualTo(RankedPlayer(PlayerId("2"), Nickname("name"), Score(10), Rank(1), 1))
            assertThat(result.count).isEqualTo(31)
        }
    }


}
