# chess-tournament
A webapp dedicated to chess championship tournaments.

## How to run Application

Launch the command

    docker-compose up -d
    
The web application will be available at http://localhost:8080/    

### OpenAPI Documentation

Available at http://localhost:8080/open-api

## How to run Karate IT tests

    docker-compose up -d
    mvn test -f it/pom.xml

## How to run Load tests with Gatling 

Launch the command

    docker-compose up -d
    
Open the project perf in IntelliJ and run the Main class in the ChessTournamentSimulation file. The Scala plugin and download of a scala SDK will be necessary
