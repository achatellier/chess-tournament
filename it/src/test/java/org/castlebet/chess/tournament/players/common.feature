Feature: Common features

  @Clear
  Scenario: Clear players should resulting an empty list

    Given url apiUrl.chess + "tournament-players"
    When method delete
    Then status 204
    When method get
    Then status 200
    And match response == '#[0]'

  @Init
  Scenario: init rank

    Given url apiUrl.chess + "tournament-players"
    And request { nickname: "superman" }
    When method post
    * def superman_id = response._id
    Given request { nickname: "robin" }
    When method post
    * def robin_id = response._id
    Given request { nickname: "batman" }
    When method post
    * def batman_id = response._id
    Given request { nickname: "robin" }
    When method post
    * def robin_id = response._id
    Given request { nickname: "joker" }
    When method post
    * def joker_id = response._id
    Given request { nickname: "harley" }
    When method post
    * def harley_id = response._id