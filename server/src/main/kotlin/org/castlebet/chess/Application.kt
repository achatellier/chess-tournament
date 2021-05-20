package org.castlebet.chess

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
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
import java.util.concurrent.TimeUnit

val MONGO_SERVER = System.getenv("MONGO_SERVER") ?: "127.0.0.1"

val mongoSettings: MongoClientSettings = MongoClientSettings
    .builder()
    .applyConnectionString(ConnectionString("mongodb://$MONGO_SERVER:27017"))
    .applyToSocketSettings {
        it.connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
    }
    .applyToConnectionPoolSettings {
        it.maxConnectionIdleTime(10, TimeUnit.MINUTES).build()
    }
    .applyToClusterSettings {
        it.serverSelectionTimeout(1, TimeUnit.SECONDS).build()
    }
    .build()

val myModule = module {
    single { MongoPlayers(get()) as Players }
    single { KMongo.createClient(mongoSettings).coroutine as CoroutineClient }
    single { (get() as CoroutineClient).toPlayerCollection() }
}


fun Application.main() {
    install(DefaultHeaders)
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
