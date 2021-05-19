#build
FROM maven:3.8.1-jdk-11-slim as build
WORKDIR /build
COPY pom.xml pom.xml
RUN mvn -q dependency:go-offline

COPY src src

# not so easy to make testscontainers tests work with docker-compose on Windows and Linux simultaneously
RUN mvn install -DskipTests


#release
FROM openjdk:11.0.5-slim
COPY --from=build /build/target/*.jar chess-tournament.jar

RUN groupadd -r chess-tournament && useradd -r -g chess-tournament chess-tournament
USER chess-tournament

ENTRYPOINT exec java -Xmx128m -Xms8m -XshowSettings:vm -jar /chess-tournament.jar
