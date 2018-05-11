Kinesis with Zombies
========================================================

## We are [Capside](http://twitter.com/capside)

* And we are kinda nice

![Drink and learn](https://pbs.twimg.com/media/ClfvYdOXIAAj1jK.jpg:large)

## Introduction

* Here are the main [slides](http://slides.com/capside/zombies#/)


## Devel credentials

* Generate new credentials with at least access to Kinesis, Cloudwatch and DynamoDB
* Set the permissions as environment variables, in Windows:

```
SET AWS_ACCESS_KEY_ID=<your access key>
SET AWS_SECRET_ACCESS_KEY=<your secret key>
SET AWS_DEFAULT_REGION=us-west-2
``` 

* On Mac/Linux:
```
export AWS_ACCESS_KEY_ID=<your access key>
export AWS_SECRET_ACCESS_KEY=<your secret key>
export AWS_DEFAULT_REGION=us-west-2
``` 

## Using the cli to create and check the Kinesis stream

```bash
aws dynamodb delete-table --table-name Zombies
aws kinesis delete-stream --stream-name zombies
aws kinesis create-stream --stream-name zombies --shard-count 2
aws kinesis describe-stream --stream-name zombies
aws kinesis describe-stream --stream-name zombies --query StreamDescription.StreamStatus
aws kinesis get-shard-iterator --stream-name zombies --shard-id shardId-000000000000 --shard-iterator-type TRIM_HORIZON --query ShardIterator
aws kinesis get-records --shard-iterator "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

## Creating the stream using the console

* Be sure to remember the *region* you are going to use
* Set 'zombies' as the stream name and ask for a capacity of 2 shards

## Getting the source code

* If you are going to use an IDE download and install [Lombok](https://projectlombok.org/download.html)
* Visit the github [project](https://github.com/capside/aws-kinesis-zombies)
* Either download it as a zip or clone the repo with ```git clone https://github.com/capside/aws-kinesis-zombies```
* Build both projects with your IDE or using [Maven](http://maven.apache.org/download.cgi)

```
cd aws-kinesis-zombies/ZombieProducer
mvn package
cd ZombieConsumer
mvn package 
```

## Getting the binaries

* Use them if you don't want to take a look at the code
* Download them from the project [releases](https://github.com/capside/aws-kinesis-zombies/releases/tag/0.0.3)

## Gotchas

* Remember to delete both the Kinesis Stream *and* the DynamoDB table
* Kinesis *is* key sensitive with the name of the streams
* The KCL uses DynamoDB to keep track of the stream. Create alerts to monitor the corresponding table.

## Running the producer and the consumer

The next scripts will start the projects allowing you to attach an external debugger to the session. 
Run them on the folders in which the jar binaries are located.
*DISCLAIMER*: for optimum performance, of course, run at least the consumer on EC2 instances.


```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 ^
     -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=5555 --stream=zombies --region=us-west-2 --latitude=51.509865 --longitude=-0.118092
```
```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1045 ^
     -jar ZombieConsumer-0.0.3-SNAPSHOT.jar ^
     --stream=zombies --region=us-west-2
```

After launching both programs you can access the visualization using ```http://localhost:8080```. And for additional fun:

* London
```bash
java -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=3333 --stream=zombies --region=us-west-2 --latitude=51.509865 --longitude=-0.118092
``` 
* Manchester
```bash
java -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=3333 --stream=zombies --region=us-west-2 --latitude=53.4808 --longitude=-2.2426
``` 
* Edinburgh
```bash
java -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=3333 --stream=zombies --region=us-west-2 --latitude=55.9533 --longitude=-3.1883
``` 
* Madrid
```bash
java -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=3333 --stream=zombies --region=us-west-2 --latitude=40.415363 --longitude=-3.707398
``` 
* Barcelona
```bash
java -jar ZombieProducer-0.0.3-SNAPSHOT.jar ^
     --drone=3333 --stream=zombies --region=us-west-2 --latitude=41.3902 --longitude=2.15400
``` 
