# Notification Service Producer Interface

---

This section explains how to send events to ENS via Kafka.

## Sending Events to the Notification Service

A service that is sending events to the Notification Service needs a basic Kafka configuration for sending the event through the Kafka bus.

It can use the default SERDE (Serializer/Deserializer) of Kafka using String type both for keys and values. 
A `KafkaTemplate<String, String>` is enough to send events to the Notification Service. Serialization should be managed in the producer code.

A producer can use a piece of code similar to the following:

    ...

    @Value("${spring.kafka.event-topic}") // The name of the Notification Service topic in properties.yaml
    private String topic;

    @Autowired
    private KafkaTemplate<String, String> template;

    ...

    public void sendEvent(String serializedPayload) {
      template.send(topic, serializedPayload);
    }

    ...
    
The topic name is specified in the helm chart value **messaging.kafka.topic**.

Events must be in compliance with the following structure:


Field Name | Type | Description | Presence
---|---|---|---
`eventID` | String | The unique Identifier of the event. | Mandatory
`descriptor` | String | The Event descriptor that is provided by the producer. | Optional
`eventType` | String | The type of the event. | Mandatory
`tenant` | String | The tenant that is associated with the event. | Mandatory
`eventTime` | DateTime | The occurrence date and time of the event. | Mandatory
`payLoad` | String | The event body, JSON encoded. | Mandatory
`additionalInformation` | String | JSON encoded, additional information of the event. | Optional
<p align="center"> Table 1 - Event Structure </p>


The java specification for this type is:

```java
    public class NsEvent {  
      String eventID;      // Unique Identifier of the event  
      String eventType;    // Type of the event  
      Timestamp eventTime; // Occurrence Time of the event  
      String tenant;       // The tenant of the event  
      String payLoad;      // Event body JSON coded  
      String descriptor;   // Optional description of the event
      String additionalInformation;   // Optional JSON encoded, additional information of the event
    }  
```

This definition is in the library `eric-notification-service-api` The definition is in this library since 1.0.7 version.
To include that library in your project, add this dependency to the maven dependencies section:

```xml
    <dependency>
      <groupId>com.ericsson.oss.common.service</groupId>
      <artifactId>eric-oss-notification-service-api</artifactId>
      <version>1.0.10</version>
    </dependency>
```

**Note**: It is recommended to use the latest version of the `eric-oss-notification-service-api` library, which is version 1.0.9.  It is compiled with Java 8, which allows compatibility with services using that Java version. 

The `eric-oss-notification-service-api` library defines the `NsEvent` class to model an event received by the Notification Service from the Kafka message bus.
Using `NsEvent` a service can prepare and send to the Notification Service events that are in compliance with the expected event syntax.

The `NsEvent` class comes with default constructor, setters, getters, `toString`, `hash` and `equals` methods. 

The library includes also a helper static class called `NsEventBuilder`, which provides methods to build a valid instance of `NsEvent`.

    public static NsEvent build(String eventType, String tenant, Object payload, String descriptor, String additionalInformation)

This method prepares an event to be delivered to Notification Service via the Kakfa message bus. The parameters are:
- `eventType`: The event type.
- `tenant`: The event tenant.
- `payLoad`: The payload, not serialized. The payload, not serialized, which means that you must pass the object that represents the event payload directly.
- `descriptor`: The event descriptor that can be used to provide additional information for debugging/tracing an event. This parameter is optional: two overloaded build methods are available with and without this argument. 
- `additionalInformation`: JSON encoded, additional information of the event

The method returns the `NsEvent` populated with the fields expected by Notification Service. The fields `eventId` and `eventTime` are automatically populated.

The `NsEventBuilder` class also provides a helper to serialize an Object into a JSON string. This is required because the Notification Service expects events as JSON encoded strings:

    public static String serialize(Object obj)

Serialize an object into a JSON string. The object `obj` that is being serialized must be a boxed type deriving from `Object` or subclasses of `Object`.  

The method returns a string with the JSON representation of the given object.

To conclude, it is possible to use the builder class in the `eric-notification-service-api` library to prepare the serialized string that is sent into the Kafka bus, with a code like:

```java
    KafkaTemplate<String, String> template;
    String topic;

    ...

    public void sendEvent(String eventType, String tenant, Object payload, Object additionalInformation) {
      NsEvent event = NsEventBuilder.build(eventType, tenant, payload, additionalInformation );
      template.send(topic, NsEventBuilder.serialize(event));
    }
```

It's also possible to manually build the `NsEvent` using directly the `NsEvent` class. For example:

```java
    public void sendEvent(String eventType, String tenant, Object payload, Object additionalInformation) {
      NsEvent event = new NsEvent();
      event.setEventID(UUID.randomUUID().toString()); // Random UUID should be preferred to guarantee unique IDs across different producers
      event.setEventTime(getCurrentTimestamp()); // The type java.sql.Timestamp must be used
      event.setEventType(eventType);
      event.setPayLoad(serialize(payload)); // Payload must be serialized into a JSON string
      event.setTenant(tenant);
      event.setAdditionalInformation(serialize(additionalInformation));
      template.send(topic, serialize(event)); // Other JSON serializers can be used, as preferred
    }
```

This is the serialization function available in `NsEventBuilder` class. You can build your one using alternative mappers, as needed.

```java
    /**
    * Serialize in JSON a generic object.
    *
    * @param obj The object to be serialized
    * @return The string with the JSON (or null in case of failure)
    */
    private String serialize(final Object obj) {
      try {
        return new ObjectMapper().writeValueAsString(obj);
      } catch (final IOException | NullPointerException e) {
        return null;
      }
    }
```

The time stamp is managed by the following functions.

```java
    /**
    * Create a time stamp with the current date and time.
    *
    * @return The "now" time stamp
    */
    private Timestamp getCurrentTimestamp() {
      TimeZone tz = TimeZone.getTimeZone("GMT");
      SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_PATTERN);
      sdf.setTimeZone(tz);
      final String date = sdf.format(new Timestamp(System.currentTimeMillis()));
      return getStrToTimestamp(date);
    }

    /**
    * Convert a string to a time stamp.
    *
    * @param date The string with the date/time to be converted in format YYYY-MM-DD HH:MM:SS
    * @return The relevant timestamp, or null in case or wrong string
    */
    private Timestamp getStrToTimestamp(final String date) {
      try {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(SIMPLE_DATE_PATTERN);
        final Date parsedDate = dateFormat.parse(date);
        return new Timestamp(parsedDate.getTime());
      } catch (final ParseException e) {
        return null;
      }
    }
```

The code above shows the rules that must be satisfied to create a valid event for the Notification Service:
- The `eventID` must be a random UUID, or any other unique identifier in the form of a string. Random UUID is strongly recommended for ALL the producers to avoid clashes.
- The `payLoad` is sent as it is, after the JSON serialization of the relevant object.
- You must specify in addition to `eventId` and `payLoad` the `eventType` and the `tenant`. If you don't want to define an `eventType` or a `tenant`, use an empty string. When defined, these fields must match the information that us stored in some subscriptions, otherwise no notification is sent. In protocols like TMF, where an `eventType` field is present in all the messages, you can use the values the `eventType` of the protocol. For other protocols you can make up the values of the `eventType`, provided both producers and clients use them consistently. 

