package org.castlebet.chess.interfaces.http

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.PlayerResult
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.Score
import org.castlebet.chess.domain.UpdatePlayerResult
import org.koin.ktor.ext.inject
import org.slf4j.event.Level


private fun handleBadRequest(): suspend PipelineContext<Unit, ApplicationCall>.(Exception) -> Unit = { e ->
    run {
        call.respondText(e.localizedMessage, ContentType.Text.Plain, HttpStatusCode.BadRequest)
        application.log.error(e.localizedMessage, e)
    }
}

private fun handleError(): suspend PipelineContext<Unit, ApplicationCall>.(Throwable) -> Unit = { e ->
    run { // Don't to say too much to the client about the error for security reasons but log everything
        call.respondText("Internal Server Error : see logs for more details", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        application.log.error(e.localizedMessage, e)
    }
}

fun Application.routes() {
    val players: Players by inject()
    install(CallLogging) {
        level = Level.TRACE
    }
    install(StatusPages) {
        exception<SerializationException>(handleBadRequest())
        exception<IllegalArgumentException>(handleBadRequest())
        exception<Exception>(handleError())
    }

    routing {
        static("openapi") {
            defaultResource("index.html", "openapi")
        }

        route("/tournament-players") {
            get {
                call.respond(HttpStatusCode.OK, players.getAll().map { it.toJson() })
            }
            post {
                val request = call.receive(JsonPlayerToCreate::class)
                call.respond(HttpStatusCode.Created, players.add(request.toPlayer()).toJson())
            }
            delete {
                players.clear()
                call.respond(HttpStatusCode.NoContent)
            }
        }
        route("/tournament-players/{id}") {
            get {
                players.get(call.pathParamToPlayerId())
                    ?.toJson()
                    ?.run { call.respond(HttpStatusCode.OK, this) }
                    ?: call.respond(HttpStatusCode.NotFound)
            }
            patch {
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

private fun PlayerToCreate.toJson() = JsonPlayerCreated(playerId.value, nickname)
private fun PlayerResult.toJson() = JsonPlayerResult(id.value, nickname, score.value)

@Serializable
data class JsonPlayerToCreate(val nickname: String) {
    fun toPlayer() = PlayerToCreate(nickname)
}

@Serializable
data class JsonPlayerCreated(val _id: String, val nickname: String)

@Serializable
data class JsonScore(val score: Int)

@Serializable
data class JsonPlayerResult(val _id: String, val nickname: String, val score: Int)
