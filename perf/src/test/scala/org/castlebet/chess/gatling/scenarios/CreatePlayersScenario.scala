package org.castlebet.chess.gatling.scenarios

import io.gatling.core.Predef
import io.gatling.core.Predef.{exec, repeat, _}
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef.{http, _}
import org.castlebet.chess.gatling.config.Config

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.util.Random

object CreatePlayersScenario {

  var ids = new ListBuffer[String]()

  def orderRef() = Random.nextInt(Integer.MAX_VALUE)

  val createPlayer: ChainBuilder = {
    exec(
      http("Insert player ranks")
        .post(session => Config.chess_url + "/tournament-players")
        .body(StringBody(session => s"""{"nickname":"""" + UUID.randomUUID().toString + """"}""")).asJson
        .check(status is 201)
        .check(jsonPath("$._id").saveAs("id"))
    )
  }


  val initIds: ChainBuilder = {
    exec(
      session => {
        //NOT_GREAT Not a great idea to use a ListBuffer, as it is not thread safe, there is a better way to do this
        //It is "working" only because the creatin rate is not high
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
