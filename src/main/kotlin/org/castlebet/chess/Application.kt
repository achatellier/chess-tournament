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

val MONGO_SERVER = System.getenv("MONGO_SERVER") ?: "127.0.0.1"

val myModule = module {
    single { MongoPlayers(get()) as Players }
    //TODO better mongo settings are needed
    single { KMongo.createClient("mongodb://$MONGO_SERVER:27017").coroutine as CoroutineClient }
    single { (get() as CoroutineClient).toPlayerCollection() }
}


fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json)
    }
}


fun main(args: Array<String>) {
    startKoin {
        modules(myModule)
    }
    embeddedServer(Netty, commandLineEnvironment(args)).start()
}
