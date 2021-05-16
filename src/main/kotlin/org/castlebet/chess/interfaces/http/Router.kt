package org.castlebet.chess.interfaces.http

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.infrastructure.persistence.MongoPlayers.UpdatePlayerResult
import org.koin.ktor.ext.inject


fun Application.routes() {
    val players: Players by inject()

    install(StatusPages) {
        exception<SerializationException> { e -> handleBadRequest(e, call) }
        exception<IllegalArgumentException> { e -> handleBadRequest(e, call) }
        exception<Throwable> { e -> call.respondText(e.localizedMessage, ContentType.Text.Plain, HttpStatusCode.Created) }
    }

    routing {
        route("/tournament-players") {
            get {
                log.info("GET Method")
                call.respond(HttpStatusCode.OK, players.getAll().map { it.toJson() })
            }
            post {
                log.info("POST Method")
                val request = call.receive(JsonPlayerToCreate::class)
                call.respond(HttpStatusCode.Created, players.add(request.toPlayer()).toJson())
            }
            delete {
                log.info("DELETE Method")
                players.clear()
                call.respond(HttpStatusCode.NoContent)
            }
        }
        route("/tournament-players/{id}") {
            get {
                log.info("GET Method")
                players.get(call.pathParamToPlayerId())
                    ?.toJson()
                    ?.run { call.respond(HttpStatusCode.OK, this) }
                    ?: call.respond(HttpStatusCode.NotFound)
            }
            patch {
                log.info("PATCH Method")
                val request = call.receive(JsonScore::class)
                when (players.update(PlayerToUpdate(call.pathParamToPlayerId(), Score(request.score)))) {
                    UpdatePlayerResult.Success -> call.respond(HttpStatusCode.OK)
                    UpdatePlayerResult.NotFound -> call.respond(HttpStatusCode.NotFound, "id ${call.pathParamToPlayerId().value}")
                }
            }
        }
    }
}

private fun ApplicationCall.pathParamToPlayerId() = PlayerId(parameters["id"] ?: throw IllegalArgumentException("id path param is mandatory"))

suspend fun handleBadRequest(t: Throwable, call: ApplicationCall) {
    call.respondText(t.localizedMessage, ContentType.Text.Plain, HttpStatusCode.BadRequest)
}

private fun PlayerToCreate.toJson() = JsonPlayerCreated(playerId.value, nickname)
private fun PlayerResult.toJson() = JsonPlayerResult(id.value, nickname, score.value)

@Serializable data class JsonPlayerToCreate(val nickname: String) {
    fun toPlayer() = PlayerToCreate(nickname)
}
@Serializable data class JsonPlayerCreated(val _id: String, val nickname: String)
@Serializable data class JsonScore(val score: Int)
@Serializable data class JsonPlayerResult(val _id: String, val nickname: String, val score: Int)
