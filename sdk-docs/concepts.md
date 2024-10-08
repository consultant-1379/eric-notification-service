# Concepts

This section describes key concepts and terminology you need to understand to use the Ericsson Notification Service (ENS) effectively. Where possible, they are presented in the order that you will most likely encounter them.

The Notification Service collects **events** that are published by other services or applications (**producers**) on a Kafka 
message bus and distributes them as REST messages, called **notifications**, to a set of subscribed **clients**.

Therefore the actors in the Notification Service scenarios are event producing services (producers) and notification receiving services (clients). The ENS resource model consists of events, subscriptions and notifications.

>**Note **: The initial message bus being supported is Kafka. Additional message bus types such as Redis, RabbitMQ and other will be supported in the future.

## Producers

Producers send messages, which are called events to the Notification Service through the Kafka Message Bus. Notification Service can receive events on a specific Kafka topic. For more information, see Kafka documentation.

## Clients

Clients can register themselves to Notification Service by creating **subscriptions**. A subscription is a record that is stored in the Notification Service database. It includes information about the registered client and the events the client wants to receive. For more information see the "Subscriptions" section.
Notifications are sent only to clients which have at least one subscription 
matching an event through a set of criteria defined in the subscription itself.

Clients can be either:

- **External**: applications outside the perimeter of the system using the 
notification service, for example SO/IDUN. These external applications must access agent services; an agent is responsible for managing a specific protocol (for example TMF or ETSI) and is also responsible for the external connection security. 
Agents convert the specific protocols supported by the clients to create, retrieve and delete  subscriptions into the Notification Service protocol.
- **Internal**: services or applications inside the perimeter of the system 
using the Notification Service. In this case, agents and security aspects are not required. Services can register directly to the Notification Service without using an agent and receive notifications without any special security mechanism.
<!-- line break -->

## Events

Events are messages that are delivered into the Kafka message bus over a specified topic. The topic is configurable, the default topic is “event”. Events are read by the ENS and processed to produce and distribute notifications to the registered clients. The events have a fixed structure:


| Field Name | Type     | Description                                                             | Presence  |
|------------|----------|-------------------------------------------------------------------------|-----------|
| `eventID`    | String   | Unique identifier of the event.                                          | Mandatory |
| `descriptor` | String   | The detailed description of the event that is provided by the event producer. | Optional  |
| `eventType`  | String   | The type of the event.                                                       | Mandatory |
| `tenant`     | String   | The tenant associated with the event. To specify no tenant, the tenant field must be populated with an empty string. | Mandatory |
| `eventTime`  | DateTime | The date and time the event occurred.                                           | Mandatory |
| `payLoad`    | String   | The body of the event. It is JSON encoded.                                               | Mandatory |
|`additionalInformation` | String | JSON encoded, additional information of the event.               | Optional |
<p align="center"> Table 1 - Event Structure </p>
<!-- line break -->

## Subscriptions

Subscriptions are registered by the clients to inform ENS about the information that they want to receive. Subscriptions contain a 
set of conditions that the event must match so it can be further processed and generate a notification for the relevant
client. An endpoint of the client is also included. The notification is sent to this endpoint.

| Field Name         | Type     | Description                                                                  | Presence                    |
|--------------------|----------|------------------------------------------------------------------------------|-----------------------------|
| `id`                 | UUID     | A unique UUID identifier generated by ENS when a new subscription is created. | Not specified in creation   |
| `address`            | URL      | The endpoint where the notifications are sent.                       | Mandatory                   |
| `tenant`             | String   | The tenant that the event must have to match the subscription. If no tenant is expected, then the tenant field must be populated with an empty string.  | Optional (can be empty string but not null)       |
| `subscriptionFilter` | Array    | A set of filtering conditions that the event must match so it can be delivered.  | Mandatory (at least 1 item) |
<p align="center"> Table 2 - Subscription Structure </p>
<!-- line break -->
The elements of the subscriptionFilter have the following structure:

| Field Name     | Type   | Description                                                                | Presence  |
|----------------|--------|----------------------------------------------------------------------------|-----------|
| `eventType`      | String | The requested type of the event.                                            | Mandatory |
| `filterCriteria` | String | A boolean filter expression (in RSQL) that must be satisfied by the event payLoad. | Optional  |
| `fields`         | String | A list of paylaod fields to be copied into the notification.         | Optional  |
<p align="center"> Table 3 - Subscription Filter Structure </p>
<!-- line break -->

## Notifications

The Notification Service collects the events received by the Kafka message bus and transforms them into notifications. 

A notification is a REST message posted to the client. Its content is the `payLoad` field of the relevant event. If a non-empty `fields` attribute is specified in the `subscriptionFilter` of the matching subscription, the content of the `payLoad` includes only the fields specified therein. 
<!-- line break -->

## How Notification Service Works

The notification service is a consumer of events and a producer of notifications for the subscribed clients.
Client services register subscriptions in the ENS database. 
Each subscription specifies the type (`eventType`) and the owner (`tenant`) of the events that the client wants to receive. 
The client also specifies filtering conditions (`subscriptionFilter`) to reduce further the number and the size of the notifications delivered to it. 
The subscription also includes the client endpoint (`address`) where the notification will be posted.

In the following diagram, events are received and read from the Kafka message bus.
Each event has a specific event type and a tenant. 

The Notification Service reads the subscriptions that have the specific event type and tenant from the database. 

It then looks for the event that matches the filter. Subscriptions containing the filter can generate a notification.

The payload transported by the event contains another filtering attribute which specifies the fields to be delivered in the notification payload.

The notification is then sent to the REST endpoint specified in the subscription. If the notification is not sent the first time, it retries to send again. 
  
![ns-process.svg](ns-process.svg "ENS Event transformation process")

<!-- line break -->

