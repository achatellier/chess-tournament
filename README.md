# chess-tournament
A webapp dedicated to chess championship tournaments.

## How to run Application

Launch the command

    docker-compose up -d
    
The web application will be available at http://localhost:8080/    

## How to run Karate IT tests

    docker-compose up -d
    mvn test -f it/pom.xml
