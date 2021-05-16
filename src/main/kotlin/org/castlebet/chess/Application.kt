package org.castlebet.chess

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.json
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import org.castlebet.chess.domain.Players
import org.castlebet.chess.infrastructure.persistence.MongoPlayers
import org.castlebet.chess.infrastructure.persistence.toPlayerCollection
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val myModule = module {
    single { MongoPlayers(get()) as Players }
    single { KMongo.createClient("mongodb://127.0.0.1:27017").coroutine as CoroutineClient }
    single { (get() as CoroutineClient).toPlayerCollection() }
}


fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json)
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

}


fun main(args: Array<String>) {
    startKoin {
        modules(myModule)
    }
    embeddedServer(Netty, commandLineEnvironment(args)).start()
}
