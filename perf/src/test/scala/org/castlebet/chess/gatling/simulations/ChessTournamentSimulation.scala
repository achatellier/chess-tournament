package org.castlebet.chess.gatling.simulations

import io.gatling.app.Gatling
import io.gatling.core.Predef.{Simulation, _}
import io.gatling.core.config.GatlingPropertiesBuilder
import org.castlebet.chess.gatling.scenarios.{CreatePlayersScenario, GetPlayersScenario, UpdateScoreScenario}


class ChessTournamentSimulation extends Simulation {

  private val updateScoreExec = UpdateScoreScenario.updateScoresScenario.inject(
    atOnceUsers(10),
    rampUsers(40) during (20)
  )
  private val getRanksExec = GetPlayersScenario.getRanksScenario.inject(
    atOnceUsers(100),
    rampUsers(5000) during (20)
  )

  private val createPlayerExec = CreatePlayersScenario.createPlayerScenario.inject(
    rampUsers(1000) during (3)
  )

  setUp(
    createPlayerExec.andThen(updateScoreExec, getRanksExec)
  )


}

object Main {
  def main(args: Array[String]): Unit = {
    val simClass: String = "org.castlebet.chess.gatling.simulations.ChessTournamentSimulation"
    val props = new GatlingPropertiesBuilder().
      simulationClass(simClass)
    Gatling.fromMap(props.build)
  }
}
