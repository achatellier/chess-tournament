package org.castlebet.chess.tournament;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.BeforeAll;

import java.util.Objects;


class ChessTournamentTest {

    private static final String CHESS_TOURNAMENT_URL = "http://localhost:8080";

    @BeforeAll
    static void setUp() {
        System.setProperty("karate.apiUrl.chess", Objects.requireNonNullElse(System.getenv("karate.apiUrl.chess"), CHESS_TOURNAMENT_URL) + "/");
    }

    @Karate.Test
    Karate apiFeatures() {
        return Karate.run("classpath:org/castlebet/chess/tournament/players").tags("~@ignore");
    }

}
