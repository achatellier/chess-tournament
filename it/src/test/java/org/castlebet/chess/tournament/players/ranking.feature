Feature: Tests dedicated to ranking

  Background:
    * callonce read("common.feature@Clear")
    * callonce read("common.feature@Init")
    * def superman_id = superman_id
    * def robin_id = robin_id
    * def batman_id = batman_id
    * def joker_id = joker_id
    * def harley_id = harley_id
    * def players = [#(superman_id), #(robin_id), #(batman_id), #(joker_id), #(harley_id)]


  Scenario Outline: Get should handle this DC Comics Greatest Chess Tournament
    Given url apiUrl.chess + "tournament-players/" + players[<updt_p_id>]
    And request { score: <updt_score> }
    When method patch
    Given url apiUrl.chess + "tournament-players"
    When method get
    Then match response.players[0].nickname == <1_name>
    And match response.players[0].rank == <1_rank>
    And match response.players[0].score == <1_score>
    And match response.players[1].nickname == <2_name>
    And match response.players[1].score == <2_score>
    And match response.players[1].rank == <2_rank>
    And match response.players[2].nickname == <3_name>
    And match response.players[2].score == <3_score>
    And match response.players[2].rank == <3_rank>
    And match response.players[3].nickname == <4_name>
    And match response.players[3].score == <4_score>
    And match response.players[3].rank == <4_rank>
    And match response.players[4].nickname == <5_name>
    And match response.players[4].score == <5_score>
    And match response.players[4].rank == <5_rank>

    Examples:
      | updt_p_id | updt_name  | updt_score |  | 1_name     | 1_rank | 1_score | 2_name     | 2_rank | 2_score | 3_name     | 3_rank | 3_score | 4_name   | 4_rank | 4_score | 5_name  | 5_rank | 5_score |
      | 0         | "superman" | 5          |  | "superman" | 1      | 5       | "batman"   | null   | null    | "harley"   | null   | null    | "joker"  | null   | null    | "robin" | null   | null    |
      | 3         | "joker"    | 5          |  | "joker"    | 1      | 5       | "superman" | 1      | 5       | "batman"   | null   | null    | "harley" | null   | null    | "robin" | null   | null    |
      | 2         | "batman"   | 15         |  | "batman"   | 1      | 15      | "joker"    | 2      | 5       | "superman" | 2      | 5       | "harley" | null   | null    | "robin" | null   | null    |

