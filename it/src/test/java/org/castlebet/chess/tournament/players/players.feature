Feature: Player management

  Background:
    * call read("common.feature@Clear")

  Scenario: Add a player should be successful

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    Then status 201

  Scenario: Add a player two times with same nickname should fail

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    Then status 201
    And request { nickname: "superman" }
    When method post
    Then status 400
    And match response == "Nickname superman already exists"

  Scenario Outline: Post with <request> should return bad request

    Given url apiUrl.chess + "tournament-players"
    And request <request>
    When method post
    Then status 400

    Examples:
      | request             |
      | {"nickname": null}  |
      | {"nickname": ""}    |
      | {"nickname": 1}     |
      | {"nickname": true}  |
      | {"unknown": "test"} |


  Scenario: Multiple players without score should be retrieved with get ordered by nickname

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    And request { nickname: "batman" }
    When method post
    When method get
    Then status 200
    And match response[0].nickname == "batman"
    And match response[0].score == '#notpresent'
    And match response[0].rank == '#notpresent'
    And match response[1].nickname == "superman"
    And match response[1].score == '#notpresent'
    And match response[1].rank == '#notpresent'


  Scenario: Single player should be retrieved with get

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    Then status 201
    * def identifier = response._id
    Given url apiUrl.chess + "tournament-players/" + identifier
    When method get
    Then status 200
    And match response.nickname == "superman"
    And match response.score == '#notpresent'

  Scenario: Single player with score should be retrieved with get

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    Then status 201
    * def identifier = response._id
    Given url apiUrl.chess + "tournament-players/" + identifier
    And request { score: 15 }
    When method patch
    Then status 200
    Given url apiUrl.chess + "tournament-players/" + identifier
    When method get
    Then status 200
    And match response.nickname == "superman"
    And match response.score == 15


  Scenario: Get should return not found for unknown players

    Given url apiUrl.chess + "tournament-players/unknown"
    When method get
    Then status 404

  Scenario: Patch should update score for a single player

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    Then status 201
    And request { nickname: "batman" }
    When method post
    Then status 201
    * def identifier = response._id
    Given url apiUrl.chess + "tournament-players/" + identifier
    And request { score: 15 }
    When method patch
    Then status 200
    And request { score: 25 }
    When method patch
    Then status 200
    Given url apiUrl.chess + "tournament-players"
    When method get
    Then status 200
    And match response[0].nickname == "superman"
    And match response[0].score == '#notpresent'
    And match response[1].nickname == "batman"
    And match response[1].score == 25

  Scenario Outline: Patch with <request> should return bad request

    Given url apiUrl.chess + "tournament-players/whatever"
    And request <request>
    When method patch
    Then status 400

    Examples:
      | request             |
      | {"score": null}     |
      | {"score": true}     |
      | {"unknown": "test"} |

