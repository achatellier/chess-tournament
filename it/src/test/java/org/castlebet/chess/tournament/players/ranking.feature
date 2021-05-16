Feature: Tests dedicated to ranking

  Background:
    * callonce read("common.feature@Clear")
    * callonce read("common.feature@Init")


  Scenario Outline: Get should handle this DC Comics Greatest Chess Tournament
    Given url apiUrl.chess + "tournament-players/<updated_player_id>"
    And request { score: <updated_player_score> }
    When method patch
    Given url apiUrl.chess + "tournament-players"
    When method get
    Then match response[<superman_order>].nickname == "superman"
    And match response[<superman_order>].rank == <superman_rank>
    And match response[<superman_order>].score == <superman_score>
    And match response[<batman_order>].nickname == "batman"
    And match response[<batman_order>].score == <batman_score>
    And match response[<batman_order>].rank == <batman_rank>
    And match response[<robin_order>].nickname == "robin"
    And match response[<robin_order>].score == <robin_score>
    And match response[<robin_order>].rank == <robin_rank>
    And match response[<harley_order>].nickname == "harley"
    And match response[<harley_order>].score == <harley_score>
    And match response[<harley_order>].rank == <harley_rank>
    And match response[<joker_order>].nickname == "joker"
    And match response[<joker_order>].score == <joker_score>
    And match response[<joker_order>].rank == <joker_rank>

    Examples:
      | updated_player_id | updated_player_score |  | superman_score | superman_rank | superman_order | batman_score  | batman_rank   | batman_order | robin_score   | robin_rank    | robin_order | harley_score  | harley_rank   | harley_order | joker_score   | joker_rank    | joker_order |
      | #(superman_id)    | 5                    |  | 5              | '#notpresent' | 0              | '#notpresent' | '#notpresent' | 1            | '#notpresent' | '#notpresent' | 4           | '#notpresent' | '#notpresent' | 2            | '#notpresent' | '#notpresent' | 3           |
      | #(joker_id)       | 5                    |  | 5              | 1             | 1              | '#notpresent' | '#notpresent' | 2            | '#notpresent' | '#notpresent' | 4           | '#notpresent' | '#notpresent' | 3            | 5             | 1             | 0           |
      | #(batman_id)      | 15                   |  | 5              | 2             | 0              | 15            | 1             | 0            | '#notpresent' | '#notpresent' | 4           | '#notpresent' | '#notpresent' | 3            | 5             | 2             | 1           |
