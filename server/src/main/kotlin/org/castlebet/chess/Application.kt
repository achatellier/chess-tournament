package org.castlebet.chess

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.castlebet.chess.domain.Players
import org.castlebet.chess.domain.RankedPlayers
import org.castlebet.chess.infrastructure.persistence.MongoPlayers
import org.castlebet.chess.infrastructure.persistence.MongoRankedPlayers
import org.castlebet.chess.infrastructure.persistence.PlayerCollection
import org.castlebet.chess.infrastructure.persistence.RankedPlayerCollection
import org.castlebet.chess.infrastructure.persistence.toPlayerCollection
import org.castlebet.chess.infrastructure.persistence.toRankedPlayerCollection
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.util.concurrent.TimeUnit

val MONGO_SERVER = System.getenv("MONGO_SERVER") ?: "127.0.0.1"

val mongoSettings: MongoClientSettings = MongoClientSettings
    .builder()
    .applyConnectionString(ConnectionString("mongodb://$MONGO_SERVER:27017"))
    .applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build() }
    .applyToConnectionPoolSettings { it.maxConnectionIdleTime(10, TimeUnit.MINUTES).build() }
    .applyToClusterSettings { it.serverSelectionTimeout(1, TimeUnit.SECONDS).build() }
    .build()

val myModule = module {
    single { MongoRankedPlayers(get(named("rankedPlayers"))) as RankedPlayers }
    single { MongoPlayers(get(named("players"))) as Players }
    single { KMongo.createClient(mongoSettings).coroutine as CoroutineClient }
    single(named("players")) { (get() as CoroutineClient).toPlayerCollection() as PlayerCollection }
    single(named("rankedPlayers")) { (get() as CoroutineClient).toRankedPlayerCollection() as RankedPlayerCollection }
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
