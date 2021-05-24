package org.castlebet.chess.interfaces.http

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
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
import org.castlebet.chess.domain.RankedPlayersResult
import org.castlebet.chess.domain.Nickname
import org.castlebet.chess.domain.Page
import org.castlebet.chess.domain.Player
import org.castlebet.chess.domain.PlayerId
import org.castlebet.chess.domain.RankedPlayer
import org.castlebet.chess.domain.PlayerToCreate
import org.castlebet.chess.domain.PlayerToUpdate
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.RankedPlayers
import org.castlebet.chess.domain.ScoreToUpdate
import org.castlebet.chess.domain.UpdatePlayerResult
import org.castlebet.chess.domain.UpdateRanksRequest
import org.castlebet.chess.domain.toRanked
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
    val rankedPlayers: RankedPlayers by inject()
    install(CallLogging) {
        level = Level.TRACE
    }
    install(StatusPages) {
        exception<SerializationException>(handleBadRequest())
        exception<IllegalArgumentException>(handleBadRequest())
        exception<Exception>(handleError())
    }
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible.
    }

    routing {

        route("/tournament-players") {
            get {
                call.respond(HttpStatusCode.OK, rankedPlayers.getAll(call.toPage() ?: Page()).toJson())
            }
            post {
                val creationResult = players.add(call.receive(JsonPlayerToCreate::class).toPlayer())
                call.respond(HttpStatusCode.Created, creationResult.createdPlayer.toJson())
                    .also { rankedPlayers.update(creationResult.toRanked()) }
            }
            delete {
                players.clear()
                rankedPlayers.clear()
                call.respond(HttpStatusCode.NoContent)
            }
        }
        route("/tournament-players/{id}") {
            get {
                rankedPlayers.get(call.pathParamToPlayerId())
                    ?.toJson()
                    ?.run { call.respond(HttpStatusCode.OK, this) }
                    ?: call.respond(HttpStatusCode.NotFound)
            }
            patch {
                val request = call.receive(JsonScore::class)
                when (val result = players.update(PlayerToUpdate(call.pathParamToPlayerId(), ScoreToUpdate(request.score)))) {
                    is UpdatePlayerResult.Success ->  call.respond(HttpStatusCode.OK).also { rankedPlayers.update(result.toRanked()) }
                    is UpdatePlayerResult.NotFound -> call.respond(HttpStatusCode.NotFound, "id ${call.pathParamToPlayerId().value}")
                }
            }
        }
        static("openapi") {
            defaultResource("index.html", "openapi")
        }
        static("/") {
            resources("dist")
            defaultResource("index.html", "dist")
        }

    }
}

private fun ApplicationCall.pathParamToPlayerId() = PlayerId(parameters["id"] ?: throw IllegalArgumentException("id path param is mandatory"))
private fun ApplicationCall.toPage() = request.queryParameters["page"]?.let { Page(it) }

private fun Player.toJson() = JsonPlayerCreated(id.value, nickname.value,)
private fun RankedPlayer.toJson() = JsonPlayerResult(id.value, nickname.value, score?.value, rank?.value)
private fun RankedPlayersResult.toJson() = JsonGetPlayerResult(count, rankedPlayers.map { it.toJson() })

@Serializable
data class JsonPlayerToCreate(val nickname: String) {
    fun toPlayer() = PlayerToCreate(Nickname(nickname))
}

@Serializable
data class JsonPlayerCreated(val _id: String, val nickname: String)

@Serializable
data class JsonScore(val score: Int)

@Serializable
data class JsonPlayerResult(val _id: String, val nickname: String, val score: Int?, val rank: Int?)
@Serializable
data class JsonGetPlayerResult(val count: Int, val players: List<JsonPlayerResult>)
