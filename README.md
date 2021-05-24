# chess-tournament
A webapp dedicated to chess tournaments.

## How to run Application

Launch the command

    docker-compose up -d
    
The frontend will be available at http://127.0.0.1:8080/    
It's a very simple read-only frontend, to create some data, you can use the Gatling load test in the "perf" directory or the Karate tests in "it" directory. 


### OpenAPI Documentation

Available at http://127.0.0.1:8080/open-api

## How to run Karate IT tests

    docker-compose up -d
    mvn test -f it/pom.xml

## How to run Load tests with Gatling 

Launch the command

    docker-compose up -d
    
Open the project perf in IntelliJ and run the Main class in the ChessTournamentSimulation file. The Scala plugin and the download of a scala SDK will be necessary


## Explanations about storage model and transactionId

###Computed pattern

To sort and rank the players, instead of sorting data at each read operation, everything is computed and stored in the database each time a score update occurs.

See : https://www.mongodb.com/blog/post/building-with-patterns-the-computed-pattern

###Transactions

A single score modification can change the rank of all players and thoses changes have to be made in a single transaction. One way to do that, is to store the tournament data in a single document. 

On other way would be to use multi-document transaction
https://docs.mongodb.com/manual/core/write-operations-atomicity/ but it is recommended to rather model the data correctly to minimize the need for multi-document transactions.

### TransactionId

The rank computation is done in 3 steps:
1. Update a score player and read all scores from a single document (atomic)
2. Compute and store all ranks in a single document (atomic)

If two computations A and B are executed at the same time, an ideal execution scenario would be : A1 -> A2 -> B1 -> B2

Unfortunately this scenario can happen:

A1 -> B1 -> B2 -> A2

In a trivial implementation, the A computation would not be aware of the score modification which happened in B1 and would override all ranks like if B1 never happened.

To avoid that, each computation has a unique incremental transactionId, computed and stored during the atomic update in the step 1. The step 2 is then only executed if the transactionId is more recent than the last one.
