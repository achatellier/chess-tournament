package org.castlebet.chess

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.routing.Routing
import io.ktor.serialization.json
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import org.castlebet.chess.domain.Players
import org.castlebet.chess.infrastructure.persistence.MongoPlayers
import org.koin.core.context.startKoin
import org.koin.dsl.module

val myModule = module {
    single { MongoPlayers() as Players }
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
