package org.castlebet.chess.domain

import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PlayerTest : WithAssertions {

    @ValueSource(strings = ["z", "-1", "1.0", "true"])
    @ParameterizedTest
    fun `Page constructor should fail if value is not a positive number`(value: String) {
        assertThatThrownBy {
            Page(value)
        }.hasMessage("A page value should be a number >= 1")
    }

    @Test
    fun `Score should be a postive number`() {
        assertThatThrownBy {
            ScoreToUpdate(-1)
        }.hasMessage("A player's score should be a positive number")
    }

    @Test
    fun `sublist should work as expected`() {
        assertThat(listOf(0).subList(Page("1"))).isEqualTo(listOf(0))
        assertThat(IntRange(1, 100).toList().subList(Page("1"))).isEqualTo(IntRange(1, 30).toList())
        assertThat(IntRange(1, 100).toList().subList(Page("2"))).isEqualTo(IntRange(31, 60).toList())
        assertThat(IntRange(1, 100).toList().subList(Page("3"))).isEqualTo(IntRange(61, 90).toList())
        assertThat(IntRange(1, 100).toList().subList(Page("4"))).isEqualTo(IntRange(91, 100).toList())
    }


    @Test
    fun `computeRank should sort and get the right ranks`() {
        val players = CreatedPlayerResult(
            1,
            Player(PlayerId("1"), Nickname("superman"), null),
            listOf(
                Player(PlayerId("1"), Nickname("superman"), null),
                Player(PlayerId("2"), Nickname("batman"), Score(20)),
                Player(PlayerId("3"), Nickname("wonderwoman"), Score(20)),
                Player(PlayerId("4"), Nickname("joker"), Score(50)),
                Player(PlayerId("5"), Nickname("robin"), null)
            )
        )

        val rankedPlayers = players.toRanked().rankedPlayers
        assertThat(rankedPlayers[0].nickname).isEqualTo(Nickname("joker"))
        assertThat(rankedPlayers[0].rank).isEqualTo(Rank(1))
        assertThat(rankedPlayers[1].nickname).isEqualTo(Nickname("batman"))
        assertThat(rankedPlayers[1].rank).isEqualTo(Rank(2))
        assertThat(rankedPlayers[2].nickname).isEqualTo(Nickname("wonderwoman"))
        assertThat(rankedPlayers[2].rank).isEqualTo(Rank(2))
        assertThat(rankedPlayers[3].nickname).isEqualTo(Nickname("robin"))
        assertThat(rankedPlayers[3].rank).isNull()
        assertThat(rankedPlayers[4].nickname).isEqualTo(Nickname("superman"))
        assertThat(rankedPlayers[4].rank).isNull()

    }

}
