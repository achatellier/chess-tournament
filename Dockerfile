FROM node:14-alpine as build-client

WORKDIR /app

COPY ./client/package.json ./
COPY ./client/package-lock.json ./
RUN npm install

COPY ./client/ ./
RUN npm run build

#build

FROM maven:3.8.1-jdk-11-slim as build


WORKDIR /app/server
COPY ./server/pom.xml ./
RUN mvn -q dependency:go-offline

COPY ./server/ ./
COPY --from=build-client /app/dist/ ./src/main/resources/dist

# not so easy to make testscontainers tests work with docker-compose on Windows and Linux simultaneously
RUN mvn install -DskipTests


#release
FROM openjdk:11.0.5-slim
COPY --from=build /app/server/target/*.jar chess-tournament.jar

RUN groupadd -r chess-tournament && useradd -r -g chess-tournament chess-tournament
USER chess-tournament

ENTRYPOINT exec java -Xmx512m -Xms8m -XshowSettings:vm -jar /chess-tournament.jar
