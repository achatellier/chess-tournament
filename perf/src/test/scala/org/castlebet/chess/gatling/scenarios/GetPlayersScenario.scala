package org.castlebet.chess.gatling.scenarios

import io.gatling.core.Predef
import io.gatling.core.Predef.{StringBody, exec, findCheckBuilder2ValidatorCheckBuilder, repeat, _}
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef.{http, status, _}
import org.castlebet.chess.gatling.config.Config

import scala.util.Random

object GetPlayersScenario {

  val getRanks: ChainBuilder = repeat(5) {
    exec(
      http("Get player ranks")
        .get(session => Config.chess_url + "/tournament-players")
        .check(status is 200))
      .pause(1, 2)
  }


  val getRanksScenario: ScenarioBuilder =
    Predef.scenario("Get player ranks")
      .exec(getRanks)
      .exec()


}
