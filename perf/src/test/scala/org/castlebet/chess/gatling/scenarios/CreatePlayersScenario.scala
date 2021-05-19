package org.castlebet.chess.gatling.scenarios

import io.gatling.core.Predef
import io.gatling.core.Predef.{exec, repeat, _}
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef.{http, _}
import org.castlebet.chess.gatling.config.Config

import java.util.UUID
import scala.collection.mutable.ListBuffer

object CreatePlayersScenario {

  var ids = new ListBuffer[String]()

  val createPlayer: ChainBuilder = {
    exec(
      http("Insert player ranks")
        .post(session => Config.chess_url + "/tournament-players")
        .body(StringBody("""{"nickname":"""" + UUID.randomUUID().toString + """"}""")).asJson
        .check(status is 201)
        .check(jsonPath("$._id").saveAs("id"))
    )
  }


  val initIds: ChainBuilder = {
    exec(
      session => {
        ids += session("id").as[String]
        session
      }
    )
  }

  val createPlayerScenario: ScenarioBuilder =
    Predef.scenario("Create Scenario")
      .exec(createPlayer, initIds)
      .exec()


}
