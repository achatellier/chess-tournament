package org.castlebet.chess.gatling.scenarios

import io.gatling.core.Predef
import io.gatling.core.Predef.{StringBody, exec, findCheckBuilder2ValidatorCheckBuilder, repeat, _}
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef.{http, status, _}
import org.castlebet.chess.gatling.config.Config

import scala.util.Random

object UpdateScoreScenario {

  val updateScore: ChainBuilder = repeat(2) {
    exec(
      http("Update scores")
        .patch(session => Config.chess_url + "/tournament-players/" + CreatePlayersScenario.ids(new Random().nextInt(CreatePlayersScenario.ids.length-1)))
        .body(StringBody(session => """{ "score":""" + new Random().nextInt(100) + "}")).asJson
        .check(status is 200))
      .pause(2, 8)
  }

  val updateScoresScenario: ScenarioBuilder =
    Predef.scenario("Update score Scenario")
      .exec(updateScore)
      .exec()


}
