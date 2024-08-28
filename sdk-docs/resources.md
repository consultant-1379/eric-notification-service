# Resources

The subscription management API includes resources and contract tests for them.

### Contract Tests

The Notification Service API stub is a way to test clients against the Notification Service.
For every REST API, a contract test stub artifact is built. The contract tests are implemented using the Spring Cloud Contract framework. 
<br/>
To run the stubs:

- Download the stub runner:<br/>

```
wget -O stub-runner.jar 'https://search.maven.org/remote_content?g=org.springframework.cloud&a=spring-cloud-contract-stub-runner-boot&v=2.2.4.RELEASE'
```

- Run the stubs:<br/>

```
java -jar stub-runner.jar --stubrunner.ids=com.ericsson.oss.common.service:eric-oss-notification-service-api:<VERSION_FROM_POM>:<ANY_PORT> -stubrunner.repositoryRoot="~/.m2/repository"  --stubrunner.stubsMode=LOCAL
```
- Send requests to the stubs and verify that it behaves as expected. Requests can be sent using utilities such as **Postman** and **curl**, or your client application. 

**Contract tests stubs** can be found [here](https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases).

### Enhanced Test Environment

The stubs have a fixed behavior. They always reply the same response to the same type of request, unregarding the actual request parameters. <br/>

An installation of the Notification Service with its runtime dependencies (see [Notification Service Dependencies](dependencies.md)) is required to have a less limited test platform for client or producer development and testing.<br/>

To launch the Notification Service in a local environment, you must have the tools **docker** and **docker-compose** installed on your machine.<br/>
You also need an access to a repository (Nexus) with Notification Service, Postgres, Zookeeper, and Kafka. 
The following procedure is tested on Linux platform (Ubuntu 20.04).

- Create an empty folder in your machine and create there a file named `docker-compose.yaml`
- Copy the following yaml text into it:

```
version: '3.1'
services:
  ns-db:
    image: postgres:10
    user: root
    volumes:
       - ./dbmaster:/docker-entrypoint-initdb.d
       - ./data:/var/lib/postgresql/data
    hostname: ns-db
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=Postgres1#
    ports:
      - "5432:5432"

  ns:
    image: armdocker.rnd.ericsson.se/proj-eo/common/proj-notification-service-drop/eric-oss-notification-service:1.0.0-168
    hostname: ns
    depends_on:
      - ns-db
      - kafka1
    environment:
      - KAFKA_BOOTSTRAP_SERVERS=PLAINTEXT://kafka1:9092
      - KAFKA_CONSUMER_AUTO_OFFSET_RESET=latest
      - KAFKA_FETCH_MAX_WAIT=500
      - KAFKA_FETCH_MIN_SIZE=1
      - KAFKA_REPLICATION_FACTOR=1
      - DB_URL=jdbc:postgresql://ns-db:5432/nsdb
      - DB_USER=postgres
      - DB_PASSWORD=Postgres1#
      - JAVA_OPTS=-DREST_READ_TIMEOUT=30000 -DKAFKA_REPLICATION_FACTOR=1 -DKAFKA_CONSUMER_AUTO_OFFSET_RESET=earliest -Dserver.port=8080 -Dspring.profiles.active=external-db -Xdebug -Xrunjdwp:transport=dt_socket,address=*:8001,server=y,suspend=n
    ports:
      - "8080:8080"
      - "8001:8001"

  zoo1:
    image: zookeeper:3.4.9
    hostname: zoo1
    ports:
      - "2181:2181"
    environment:
      ZOO_MY_ID: 1
      ZOO_PORT: 2181
      ZOO_SERVERS: server.1=zoo1:2888:3888
    volumes:
      - ./zk-single-kafka-single/zoo1/data:/data
      - ./zk-single-kafka-single/zoo1/datalog:/datalog

  kafka1:
    image: confluentinc/cp-kafka:5.5.1
    hostname: kafka1
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: LISTENER_DOCKER_INTERNAL://kafka1:9092 
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: LISTENER_DOCKER_INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181"
      KAFKA_BROKER_ID: 1
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    volumes:
      - ./zk-single-kafka-single/kafka1/data:/var/lib/kafka/data
    depends_on:
      - zoo1
```

- Create a subfolder called `.dbmaster` and create a file named `create-databases.sh` in it.
- Copy the following text into the file `create-databases.sh`

```
#!/bin/bash
set -e
set -u
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE USER nsdbuser;
  CREATE DATABASE nsdb;
  GRANT ALL PRIVILEGES ON DATABASE nsdb TO nsdbuser;
EOSQL
```

- Make the shell script executable. This script shall be invoked automatically to create a postgres database for the Notification Service, if one can not be found at  startup time.
- Run the following command:

```
docker-compose up -d
```

The first time you run the command above, docker-compose loads the applications into your local docker space, so it will take a while to start. 
The next runs will be faster.<br/>
After the startup you can use a client, for example **postman**, **curl** or your client application, to connect to the Notification Service on port 8080.
- To view log information from the Notification Service:

```
docker-compose logs --tail=50000 -f ns
```
- To close the test/debug session:

```
docker-compose down
```

After running docker-compose the first time, two sub-folders will be created:

- `./data` : contains the postgres data
- `./zk-single-kafka-single` : contains the Kafka/Zookeeper data


These folders guarantee the persistence of the data across testing/debugging sessions. 
If you want to start a session from scratch, delete them before restarting the docker-compose. They are owned by root, so use:
```
sudo /bin/rm -rf data
sudo /bin/rm -rf zk-single-kafka-single
```
to remove them, as needed.