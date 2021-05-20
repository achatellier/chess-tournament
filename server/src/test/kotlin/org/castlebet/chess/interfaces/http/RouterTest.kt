package org.castlebet.chess.interfaces.http

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.WithAssertions
import org.castlebet.chess.domain.GetPlayerResult
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Page
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.castlebet.chess.main
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RouterTest : WithAssertions {

    private val players: Players = mockk()

    @BeforeEach
    fun before() {
        clearMocks(players)
        stopKoin()
    }

    private fun <T> testApp(test: TestApplicationEngine.() -> T): T {
        return withTestApplication(
            {
                routes()
                main()
            },
            test.also {
                startKoin {
                    modules(module {
                        single { players }
                    })
                }
            }
        )
    }

    @ValueSource(
        strings = [
            """{"nickname": null}""",
            """{"nickname": ""}""",
            """{"nickname": 1}""",
            """{"nickname": true}""",
            """{"unknown": "test"}"""]
    )
    @ParameterizedTest
    fun `should return bad request when payload is invalid `(body: String) {
        testApp {
            coEvery { players.add(any()) } returns PlayerToCreate(
                Nickname("nickname"), PlayerId("2")
            )

            val call = handleRequest(HttpMethod.Post, "/tournament-players") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }

    @Test
    fun `should return OK when add repository response is successful and payload is valid`() {
        testApp {
            coEvery { players.add(any()) } returns PlayerToCreate(Nickname("nickname"), PlayerId("2"))

            val body = """{"nickname": "anthony"}"""
            val call = handleRequest(HttpMethod.Post, "/tournament-players") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Created)
                assertThat(response.content).isEqualTo("{\"_id\":\"2\",\"nickname\":\"nickname\"}")
            }
        }
    }

    @ValueSource(
        strings = [
            "Z",
            "",
            "-1",
            "null"]
    )
    @ParameterizedTest
    fun `should return bad request when page is not valid for getAll `(page: String) {
        testApp {
            val call = handleRequest(HttpMethod.Get, "/tournament-players?page=$page")
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }


    @Test
    fun `should return OK when getAll repository response is successful and payload is valid`() {
        testApp {
            coEvery { players.getAll(Page("1")) } returns GetPlayerResult(
                2,
                listOf(
                    PlayerResult(PlayerId("1"), Nickname("superman"), Score(15)),
                    PlayerResult(PlayerId("42"), Nickname("aquaman"), Score(123789))
                )
            )

            val call = handleRequest(HttpMethod.Get, "/tournament-players?page=1")
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                assertThat(response.content).isEqualTo(
                    Json.encodeToString(
                        JsonGetPlayerResult(
                            2,
                            listOf(
                                JsonPlayerResult("1", "superman", 15),
                                JsonPlayerResult("42", "aquaman", 123789)
                            )
                        )
                    )
                )
            }
        }
    }

    @Test
    fun `should return OK when update repository response is successful`() {
        testApp {
            coEvery {
                players.update(PlayerToUpdate(PlayerId("1"), Score(10)))
            } returns UpdatePlayerResult.Success

            val call = handleRequest(HttpMethod.Patch, "/tournament-players/1") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(JsonScore(10)))
            }
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `should return NotFound when update repository response is not found`() {
        testApp {
            coEvery {
                players.update(PlayerToUpdate(PlayerId("1"), Score(10)))
            } returns UpdatePlayerResult.NotFound

            val call = handleRequest(HttpMethod.Patch, "/tournament-players/1") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(JsonScore(10)))
            }
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NotFound)
            }
        }
    }

    @Test
    fun `should return OK when get repository response is successful`() {
        testApp {
            coEvery {
                players.get(PlayerId("1"))
            } returns PlayerResult(PlayerId("1"), Nickname("superman"), Score(15))

            val call = handleRequest(HttpMethod.Get, "/tournament-players/1")
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                assertThat(response.content).isEqualTo(Json.encodeToString(JsonPlayerResult("1", "superman", 15)))
            }
        }
    }

    @ValueSource(
        strings = [
            """{"score": null}""",
            """{"score": true}""",
            """{"unknown": "test"}"""]
    )
    @ParameterizedTest
    fun `should return Bad request when input is invalid`(body: String) {
        testApp {
            val call = handleRequest(HttpMethod.Patch, "/tournament-players/1") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }


    @Test
    fun `should return NotFound when get repository response is null`() {
        testApp {
            coEvery {
                players.get(PlayerId("1"))
            } returns null

            val call = handleRequest(HttpMethod.Get, "/tournament-players/1")
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NotFound)
            }
        }
    }

    @Test
    fun `should return NotFound when delete repository response is OK`() {
        testApp {
            coEvery { players.clear() } just Runs

            val call = handleRequest(HttpMethod.Delete, "/tournament-players")
            with(call) {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }


}
