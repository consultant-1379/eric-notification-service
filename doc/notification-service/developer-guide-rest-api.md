# Notification Service Client Interface

---

The following sections provide details about how clients can register subscriptions into ENS and receive notifications when a matching event occurs. The sections included here are:

  - [Application Programming Interface and Contract Tests](#application-programming-interface-and-contract-tests)
  - [Managing Subscriptions](#managing-subscriptions)
  - [The Filter Criteria Attribute](#the-filter-criteria-attribute)
  - [The Fields Attribute](#the-fields-attribute)
  - [Health Check Endpoint](#health-check-endpoint)
  - [Receiving Notifications from the Notification Service](#receiving-notifications-from-the-notification-service)

## <a name="application-programming-interface-and-contract-tests"></a> Application Programming Interface and Contract Tests

Notification Service provides a REST API to allow clients to register, deregister, and read subscriptions. 

### Description

Java classes are generated from an OpenAPI YAML description that is stored in the `eric-notification-service-api` project, in the relevant [Gerrit repository](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.common.service/eric-notification-service-api).

The resulting .jar artifact is released and tagged by the version of the API DSL. It's located in [Nexus repository](https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases/com/ericsson/oss/common/service/eric-oss-notification-service-api/).

Currently the following different versions of ENS and the relevant API library are released. The following table details the version compatibility between the Notification Service and the API library versions.

| ENS version | API version | Change description                                               |
|-------------|-------------|------------------------------------------------------------------|
| 1.0.0-168   | 1.0.10      | A new optional property added to NsEvent, JSON encoded additionalInformation  |
|             | 1.0.9       | Introduction of common Error Message                             |
|             | 1.0.8       | Use Java 8 to compile API library (fix)                          |
| 1.0.0-164   | 1.0.7       | First official release (with NsEvent and NsEventBuilder support) |
<p align="center"> Table 1 - ENS API Versions Compatibility </p>

The `eric-notification-service-api` repository contains API for Notification Service subscription management and event production. The details for the event production are described in [Notification Service Producer Interface](developer-guide-msg-bus-api.md).

The subscription management API includes resources and contract tests for them.

**Contract tests stubs** can be found [here](https://arm1s11-eiffel052.eiffel.gic.ericsson.se:8443/nexus/content/repositories/eo-releases).

Notification Service API is created with the following technologies:

- Java 8
- Spring Boot
- Spring Cloud Contract
- OpenAPI (Swagger)
- Maven

### Contract Tests

The Notification Service API stub is a simple way to test clients against the Notification Service.
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

### Enhanced Test Environment

The stubs have a fixed behavior. They always reply the same response to the same type of request, unregarding the actual request parameters. <br/>

An installation of the Notification Service with its runtime dependencies (see [Notification Service Dependencies](dependencies.md)) is required to have a less limited test platform for client or producer development and testing.<br/>

To launch the Notification Service in a local environment, you must have the tools **docker** and **docker-compose** installed on your machine.<br/>
You also need an access to a repository (Nexus) with Notification Service, Postgres, Zookeeper and Kafka. 
The following procedure has been tested on Linux platform (Ubuntu 20.04); using Windows or other platforms some commands could be different.

- Create an empty folder in your machine and create there a file named `docker-compose.yaml`
- Copy the following yaml text into it:
```yml
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
```sh
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

## <a name="managing-subscriptions"></a> Managing Subscriptions

Subscriptions can be managed by clients through RESTful API. 
All the endpoints and the relevant API structures are relevant to the last version of the Notification Service and its API (1.0.0-168 and 1.0.9).

See [ENS API](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Notification+Service+API)

### Create a Subscription

You can create a new subscription with the RESTful API. The subscription details must be unique and syntactically correct. If another subscription with the same parameters already exists, the request fails. The filterCriteria details and fields syntax is provided below.

**Endpoint**: POST /notification/v1/subscriptions

**RequestBody**:

```json
{
  subscriptionFilter: [
    {
      eventType: "<string>",
      filterCriteria: "<string>",
      fields: "<string>"
    },
    ...
  ],
  address: "<string>",
  tenant: "<string>"
}
```

A description of the fields in a subscription as well as the rules these fields are validated against are defined in the following table.

| Field              | Type | Description                                   | Mandatory/Optional                | Validation Rule                                              |
|--------------------|------|-------------------------------------------|-----------------------------------|--------------------------------------------------------------|
| `id`                 | String | The unique identifier of the subscription. | Not specified in the creation request. | Must be a valid UUID                                         |
| `address`            | String | The endpoint to send notifications. The set of `eventType`, `address`, `filterCriteria` and `tenant` must be unique for every subscription.       | Mandatory                         | Must be a valid URL, which includes the protocol and the host.     |
| `tenant`             | String | The tenant of the subscription. The set of `eventType`, `address`, `filterCriteria` and `tenant` must be unique for every subscription.           | Optional  (can be empty string but not null)        |                                                              |
| `subscriptionFilter` | String | A list of matching conditions.             | Mandatory (at least 1 entry)      | See the rules for the three sub-fields                           |
| `.eventType`         | String | The type of the event. The set of `eventType`, `address`, `filterCriteria` and `tenant` must be unique for every subscription.                     | Mandatory                         |                                                              |
| `.filterCriteria`    | String | A boolean RSQL expression. The set of `eventType`, `address`, `filterCriteria` and `tenant` must be unique for every subscription.                | Optional                          | Must be a valid RSQL expression                              |
| `.fields`            | String | The list of fields to include in notification. | Optional                          | Must be a comma separated list of JSON paths                 |
<p align="center"> Table 2 - Subscription Body Structure </p>

For details about the `fields` and `filterCriteria` fields syntax, see the relevant sections below.

**Response**:

**201 Created** 

```json
{    
  id: "<string>",
  subscriptionFilter: [
    {
      eventType: "<string>",
      filterCriteria: "<string>",
      fields: "<string>"
    },
    ...
  ],
  address: "<string>",
  tenant: "<string>"
}
```

It returns the full subscription record stored in the ENS database, including the identifier `id` assigned by ENS to the subscription. The subscription id is important, and should be stored by the clients in case they want to delete the subscription at a later time. 

**409 Conflict**

```json
{
  errorCode: "ENS-K-03"
  userMessage: "Error creating subscription. A subscription with the same properties already exists. Subscription id: 'subscriptionId'"
  developerMessage: "Error creating subscription. A subscription with the same properties already exists. Subscription id: 'subscriptionId'"
  errorData: [ "subscriptionId" ]
}
```

where `subscriptionId` is the identifier of the conflicting subscription found in the database.
In order to change the fields of an existing subscription, it is only possible to do so by deleting that subscription and creating a new one with the required fields.


**400 Bad Request**:

Three different errors that return Bad Request:

- Some mandatory fields are missing:
```json
{
  errorCode: "ENS-B-00"
  userMessage: "Error creating subscription. The request has missing mandatory fields: missing field"
  developerMessage: "Error creating subscription. The request has missing mandatory fields: missing field"
  errorData: [ "missing field" ]
}
```
where 'missing field' is the name of the mandatory field missing.

- Wrong JSON syntax, such as missing commas, or misplaced parenthesis, and more:
```json
{
  errorCode: "ENS-B-01"
  userMessage: "Error creating subscription. Failure parsing request: offending request"
  developerMessage: "Error creating subscription. Failure parsing request: offending request"
  errorData: [ "offending request" ]
}
```
where 'offending request' is the body JSON of the wrong request that is producing this error.

- Wrong parameter syntax: filterCriteria and/or fields parameters.
```json
{
  errorCode: "ENS-C-02"
  userMessage: "Error creating subscription. The request has wrong parameters: error details"
  developerMessage: "Error creating subscription. The request has wrong parameters: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the offending parameter syntax.

**500 Internal Server Error**

Two different error families return Internal Server Error:

- Database operations error
```json
{
  errorCode: "ENS-E-04"
  userMessage: "Error creating subscription. Database error: error details"
  developerMessage: "Error creating subscription. Database error: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the failed database operation.

- Other unexpected errors, for example, NULL pointer exception, occurred during the management of the request. These types of errors are usually bugs.
```json
{
  errorCode: "ENS-Z-05"
  userMessage: "Error creating subscription. An unexpected error occurred: error details"
  developerMessage: "Error creating subscription. An unexpected error occurred: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the relevant exception.

### Get All Subscriptions

**Endpoint**: GET /notification/v1/subscriptions 

This request has no body.

**Response**

**200 Ok**

```json
[
  {
    id: "<string>",
    subscriptionFilter: [
      {
        eventType: "<string>",
        filterCriteria: "<string>",
        fields: "<string>"
      },
      ...
    ],
    address: "<string>",
    tenant: "<string>"
  },
  ...
]
</pre>

**500 Internal Server Error**

Two different error families return Internal Server Error:

- Database operations error
```json
{
  errorCode: "ENS-E-07"
  userMessage: "Error retrieving subscription. Database error: error details"
  developerMessage: "Error retrieving subscription. Database error: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the failed database operation.

- Other unexpected errors, for example, NULL pointer exception, occurred during the management of the request. These errors are usually bugs.
```json
{
  errorCode: "ENS-Z-08"
  userMessage: "Error retrieving subscription. An unexpected error occurred: error details"
  developerMessage: "Error retrieving subscription. An unexpected error occurred: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the relevant exception.<br></br>

### Get a Subscription by Id

**Request**: GET /notification/v1/subscriptions/{id}

This request has No Body. The `{id}` path parameter is a UUID identifying in a unique way the subscription to be retrieved. 

**Response**:

**200 Ok**

```json
{
  id: "<string>",
  subscriptionFilter: [
    {
      eventType: "<string>",
      filterCriteria: "<string>",
      fields: "<string>"
    },
    ...
  ],
  address: "<string>",
  tenant: "<string>"
}
```

**400 Bad Request**

The provided subscription identifier is not a valid UUID. 
```json
{
  errorCode: "ENS-B-12"
  userMessage: "Error parsing request: error details"
  developerMessage: "Error parsing request: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error that is returned by Jackson (the JSON parser used by the Notification Service) when parsing the endpoint.

**404 Not Found**

Error retrieving subscription. A subscription with the provided id '{0}' couldn't be found.
```json
{
  errorCode: "ENS-J-06"
  userMessage: "Error retrieving subscription. A subscription with the provided id 'subscriptionId' couldn't be found"
  developerMessage: "Error retrieving subscription. A subscription with the provided id 'subscriptionId' couldn't be founds"
  errorData: [ "subscriptionId" ]
}
```
where 'subscriptionId' is the identifier that was provided in the request and couldn't be found in the ENS database.

**500 Internal Server Error**

Two different error families return Internal Server Error:

- Database operations error
```json
{
  errorCode: "ENS-E-07"
  userMessage: "Error retrieving subscription. Database error: error details"
  developerMessage: "Error retrieving subscription. Database error: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the failed database operation.

- Other unexpected errors, for example, NULL pointer exception, occurred during the management of the request. These errors are usually bugs.
```json
{
  errorCode: "ENS-Z-08"
  userMessage: "Error retrieving subscription. An unexpected error occurred: error details"
  developerMessage: "Error retrieving subscription. An unexpected error occurred: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the relevant exception.

### Delete a Subscription

**Endpoint**: DELETE /notification/v1/subscriptions/{id}

This request has no body. The `{id}` path parameter is a UUID identifying in a unique way the subscription to be deleted. 

**Response**

**204 No Body** 

The deletion completed successfully.

**400 Bad Request**

The provided subscription identifier is not a valid UUID. 
```json
{
  errorCode: "ENS-B-12"
  userMessage: "Error parsing request: error details"
  developerMessage: "Error parsing request: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error that is returned by Jackson when parsing the endpoint.

**404 Not Found** 

Error retrieving subscription. A subscription with the provided id couldn't be found.
```json
{
  errorCode: "ENS-J-09"
  userMessage: "Error deleting subscription. A subscription with the provided id 'subscriptionId' couldn't be found"
  developerMessage: "Error deleting subscription. A subscription with the provided id 'subscriptionId' couldn't be founds"
  errorData: [ "subscriptionId" ]
}
```
where subscriptionId is the identifier that is provided in the request and cannot be found in the ENS database.

**500 Internal Server Error**

Two different error families return Internal Server Error:

- Database operations error
```json
{
  errorCode: "ENS-E-10"
  userMessage: "Error deleting subscription. Database error: error details"
  developerMessage: "Error deleting subscription. Database error: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the failed database operation.

- Other unexpected errors, for example, NULL pointer exception, occurred during the management of the request. Thes errors are usually bugs.
```json
{
  errorCode: "ENS-Z-11"
  userMessage: "Error deleting subscription. An unexpected error occurred: error details"
  developerMessage: "Error deleting subscription. An unexpected error occurred: error details"
  errorData: [ "error details" ]
}
```
where 'error details' show the error text that is returned by the relevant exception.

## <a name="the-filter-criteria-attribute"></a> The Filter Criteria Attribute

The Filter Criteria attribute describes a boolean expression applicable on the event payload.
If the expression evaluates to true only, a notification is generated and sent to the address that is specified in the subscription. 
The filter expression is written in RSQL.

RSQL is a query language for parametrized filtering of entries in RESTful APIs.
It’s based on FIQL (Feed Item Query Language), a query language studied for use in URI.
There are no unsafe characters so URL encoding is not required. RSQL adds to FIQL a friendlier syntax for logical operators.

See [RSQL Parser](https://github.com/jirutka/rsql-parser) for full details.

RSQL expression is composed of one or more **comparisons** related to each other with **logical operators**:

    Logical AND: ";" or "and"
    Logical OR: "," or "or"

By default, the AND operator is evaluated before any OR operators. 
However, a parenthesized expression can be used to change the precedence, which yields whatever the contained expression yields.

    input          = or, EOF;
    or             = and, { "," , and };
    and            = constraint, { ";" , constraint };
    constraint     = ( group | comparison );
    group          = "(", or, ")";


A **Comparison** is composed of a **selector**, an **operator**, and an **argument**.

    comparison     = selector, comparison-op, arguments;


A **Selector** identifies a field of the resource representation to filter by. 
It can be any JSONPath expression applicable to the event payload and **not including** any reserved RSQL character, that is one of: **" ' ( ) ; , = ! ~ < >**

    selector       = unreserved-str;

JSONPath expressions are very powerful and add value especially when retrieving items from arrays. For example:

    event.array[*].name=='my name' 
    
    which means: return true in case there is at least one element of the array event.array with name equal to 'my name'
   
is a way to look into an array, part of a JSON payload, and search for items satisfying a given condition. It is also possible to specify a fixed index. For example:

    event.array[0].name=='my name'

    which means: return true in case the field name in the first element of the array event.array has the value 'my name'

Not all the valid JSONPath expressions can be used. For example:

    event.array[?(@.name=='my name')].status==enabled
   
    which means: return true in case there is at least one element of the array event.array with name equal to 'my name' and status equal to enabled
   
Here the expression ?(@.name=='my name') cannot be used because it includes the RSQL reserved characters **=**, **(**, **)**, and **'**. 

In this example, use instead:

    event.array[*].name=='my name';event.array[*].status==enabled    


FIQL notation supports **Comparison operators** and some of these operators also have an alternative syntax.

    Equal to : ==
    Not equal to : !=
    Less than : =lt= or <
    Less than or equal to : =le= or <=
    Greater than operator : =gt= or >
    Greater than or equal to : =ge= or >=
    In : =in=
    Not in : =out=

Notification Service adds the regular expression operator:

    Regex: =regex=

**Argument** can be a single value or multiple values in parenthesis separated by comma. 
A value that doesn’t contain any reserved character or a white space can be unquoted, other arguments must be enclosed in single or double quotes.


    arguments      = ( "(", value, { "," , value }, ")" ) | value;
    value          = unreserved-str | double-quoted | single-quoted;
    unreserved-str = unreserved, { unreserved }
    single-quoted  = "'", { ( escaped | all-chars - ( "'" | "\" ) ) }, "'";
    double-quoted  = '"', { ( escaped | all-chars - ( '"' | "\" ) ) }, '"';
    reserved       = '"' | "'" | "(" | ")" | ";" | "," | "=" | "!" | "~" | "<" | ">";
    unreserved     = all-chars - reserved - " ";
    escaped        = "\", all-chars;
    all-chars      = ? all unicode characters ?;

If you need to use both single and double quotes inside a quoted argument, then you must escape one of them using \\ (backslash). 
If you want to use \\ literally, then double it as \\\\. Backslash has a special meaning only inside a quoted argument, not in unquoted argument.

## <a name="the-fields-attribute"></a> The Fields Attribute

Sometimes a client only needs a subset of the fields of the event payload. To reduce the size of the notifications, a subscription can use the `fields` attribute, which contains a comma-separated list of all the fields to be included in the notifications matching that subscription.
The fields that are not present in the `fields` attribute are removed from the payload of the Notification. This operation is called "projection".
If the "Fields" attribute is missing, the event payload is delivered in the body of the Notification as it is.

The name of the fields are expressed in dotted form, that is, they are represented as a path in the JSON tree of the event payload with node names separated by dots.
For nodes that are arrays, it is possible to indicate the index of the array with the [index] notation.
 The index is a number >= 0, specifying which element of the array has to be output, or the symbol "*", which means that the whole array will be projected.
For example, the payload:
```json
    { 
      "eventId": "00001", 
      "eventTime": "2015-11-16T16:42:25-04:00", 
      "eventType": "ServiceOrderCreateEvent", 
      "event": { 
        "serviceOrder": { 
            "id": "42", 
            "href": "http://serverlocation:port/serviceOrdering/v4/serviceOrder/42", 
            "externalId": "BSS747", 
            "priority": "1", 
            "description": "Service order description", 
            "category": "TMF resource illustration", 
            "state": "acknowledged", 
            "orderDate": "2018-01-12T09:37:40.508Z", 
            "completionDate": "", 
            "requestedStartDate": "2018-01-15T09:37:40.508Z", 
            "requestedCompletionDate": "2018-01-15T09:37:40.508Z", 
            "expectedCompletionDate": "2018-01-15T09:37:40.508Z", 
            "startDate": "2018-01-12T09:37:40.508Z", 
            "@type": "ServiceOrder", 
            "note": [ 
                { 
                  "date": "2018-01-15T09:37:40.508Z", 
                  "author": "Harvey Poupon", 
                  "text": "Some text" 
                } 
              ], 
            "relatedParty": [ 
                { 
                  "id": "456", 
                  "href": "http://serverlocation:port/partyManagement/v4/party/456", 
                  "role": "requester", 
                  "name": "Jean Pontus", 
                  "@referredType": "Individual" 
                } 
              ], 
            "serviceOrderItem": [ 
                { 
                  "id": "1", 
                  "action": "add", 
                  "state": "acknowledged", 
                  "service": { 
                    "@type": "Service", 
                    "name": "sample service1", 
                    "serviceType": "RFS", 
                    "serviceCharacteristic": [ 
                        { 
                          "name": "bandwidth", 
                          "valueType": "String", 
                          "value": "100Mbps" 
                        }, 
                        { 
                          "name": "subsystemName", 
                          "valueType": "String", 
                          "value": "ecm" 
                        }         
                      ], 
                    "serviceSpecification": { 
                      "id": "90ce2704-6366-469b-8373-ce0671c9c845",
                      "name": "TMF_ServiceTemplate",
                      "version": "1"               
                    }             
                  }           
                },
                 { 
                  "id": "2", 
                  "action": "add", 
                  "state": "acknowledged", 
                  "service": { 
                    "@type": "Service", 
                    "name": "sample service2", 
                    "serviceType": "RFS", 
                    "serviceCharacteristic": [ 
                        { 
                          "name": "bandwidth", 
                          "valueType": "String", 
                          "value": "200Mbps" 
                        }, 
                        { 
                          "name": "subsystemName", 
                          "valueType": "String", 
                          "value": "ecm" 
                        }         
                      ], 
                    "serviceSpecification": { 
                      "id": "90ce2704-6366-469b-8373-ce0671c9c846",
                      "name": "TMF_ServiceTemplate",
                      "version": "1"               
                    }             
                  }           
                }               
              ]
          }
        }
    }
```
If the `fields` attribute is

    "eventId,eventType,event.serviceOrder.id,event.serviceOrder.state,event.serviceOrder.serviceOrderItem[0]"

it becomes:
```json
    { 
      "eventId": "00001", 
      "eventType": "ServiceOrderCreateEvent", 
      "event": { 
        "serviceOrder": 
         { 
            "id": "42", 
            "state": "acknowledged", 
            "serviceOrderItem": [ 
                { 
                  "id": "1", 
                  "action": "add", 
                  "state": "acknowledged", 
                  "service": { 
                    "@type": "Service", 
                    "name": "sample service1", 
                    "serviceType": "RFS", 
                    "serviceCharacteristic": [ 
                        { 
                          "name": "bandwidth", 
                          "valueType": "String", 
                          "value": "100Mbps" 
                        }, 
                        { 
                          "name": "subsystemName", 
                          "valueType": "String", 
                          "value": "ecm" 
                        }         
                      ], 
                    "serviceSpecification": { 
                      "id": "90ce2704-6366-469b-8373-ce0671c9c845",
                      "name": "TMF_ServiceTemplate",
                      "version": "1"               
                    }             
                  }           
                }
              ]
          }
        }
    }
```
If the `fields` attribute is

    "eventId,eventType,event.serviceOrder.id,event.serviceOrder.state,event.serviceOrder.serviceOrderItem[*].id"

it becomes:
```json
    { 
      "eventId": "00001", 
      "eventType": "ServiceOrderCreateEvent", 
      "event": { 
        "serviceOrder": 
         { 
            "id": "42", 
            "state": "acknowledged", 
            "serviceOrderItem": [ 
                { 
                  "id": "1"
                },
                {
                  "id": "2"
                }
              ]
          }
        }
    }
```
**Note**: The Notification Service does not know any details of the delivered payload. Therefore, it cannot ensure that the projection produces a valid output, for example, there might be some mandatory attributes missing after the projection. 
Storing `fields` attributes to produce correct notification payloads is the responsibility of the clients because the contract of the notification is between the client and the producer. The Notification Service is just the broker.


## <a name="health-check-endpoint"></a> Health Check Endpoint

To verify that OSS Notification Service service is healthy, use the following command: 

    curl -i http://<ip_address>:<port>/actuator/health

where \<ip_address\> is the service host and \<port\> is the service port.
For example:

curl -i http://eric-oss-notification-service:8080/actuator/health

It returns HTTP 200 Ok with body:
```json
    {
      "status": "UP"
    }
```
if the service is running correctly.

## <a name="receiving-notifications-from-the-notification-service"></a> Receiving Notifications from the Notification Service

A client application subscribing for notifications with the Notification Service need to expose a REST endpoint (specified in the `address` attribute of the subscription) to receive the notifications.
The Notification Service sends a POST to that endpoint. The body of the POST includes the `payLoad` of the event, which is reduced according to the `fields` attribute. See the **The Field Attribute** section above. 


The client code must respond quickly to the Notification service, postponing time-consuming operations. 

The client responding quickly is important for the following reasons:

- Slow or unresponsive clients slow down Notification Service operations, affecting all the cients.
- The Notification Service has configured timeouts for connection and read operation with the clients. If those timeouts are exceeded, the Notification Service will retry delivering the notification and, if the problem persists, it will consider the notification as failed.
- There is a higher probability of duplicate notifications in case the Notification Service restarts while waiting for client responses.


However, duplicated notifications cannot be completely avoided on the REST interface between the Notification Service and the client. 
If the Notification Service is waiting for the client to respond and while waiting, it shuts down, then it assumes that the notification is not delivered or received, and therefore, after the restart, it will resubmit the notification. This is another case of the "Two generals problem". Nonetheless, the state of one client does not impact the delivery of the notification to another client.


If some errors occur in the client during the processing of the notification, HTTP error codes must be returned to Notification Service.
In case a 4xx error is returned, the Notification Service considers the delivery completed.
In case a 5xx error is returned, the Notification Service tries and retransmit the notification, assuming a temporary issue.

