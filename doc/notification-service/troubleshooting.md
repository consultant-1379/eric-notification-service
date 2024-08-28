# Notification Service Troubleshooting

---

This section shows how to investigate and solve the most common problems encountered running ENS.

  - [REST Error Responses](#rest-error-responses)
  - [Expected Notifications Not Received](#expected-notifications-not-received)
  - [Unexpected Notifications Received](#unexpected-notifications-received)

## <a name="rest-error-responses"></a>REST Error Responses

ENS does not have a GUI, so all operations on the REST interface are completed by other services, either agents or clients. <br/>
When an error occurs serving a request from another service, Notification Service returns an HTTP error with a body like this:
```json
{
  errorCode: "<Code of the error with the format <Service trigram (ENS)>-<Error Type letter>-<Numeric error code>. For example: ENS-B-00>"
  userMessage: "A full description of the error, including all the available details"
  developerMessage: "Additional debugging content (at the moment it is equal to the userMessage field)"
  errorData: [ "<A set of strings with additional information about the problem, for example a network error message or a stack dump" ]
}
```
In general in these cases, with the exception of unpredictable errors ("Z" code family) or database errors ("E" code family), investigations should be conducted on the caller code.

|Error Code | Problem          | Solution          |
|-----------|------------------|-------------------|
| **ENS-B-00**<br/> Mandatory Fields Missing | There are mandatory fields missing while creating the subscription.<br/> As a result, the subscription is not created successfully. <br/> <br/> The error message returned to the caller is: <br/> "**Error creating subscription. The request has missing mandatory fields: {missing fields}**" | The code or the content of the data used to create the request to ENS should be reviewed to ensure all mandatory fields are present. Mandatory fields are:<br/>  - `tenant` : can be empty string but not null <br/>  - `address`<br/>  - `eventType`<br/>  - `subscriptionFilter` : must have at least one entry.<br/>  |
| **ENS-B-01**<br/> Syntax Error in Request | There was an error creating the subscription because a syntax error is detected in the body of a create request.<br/><br/> The error message returned to the caller is:<br/> "**Error creating subscription. Failure parsing request: {parsing exception text}**" | Check the parsing exception text to understand the root cause. <br/><br/> **Note**: this error is actually never returned, as ENS-B-12 (which also includes issues in the URL itself) is always triggered instead. |
| **ENS-C-02**<br/> Invalid Parameters Error | There was an error creating the subscription because the request has the wrong `filterCriteria` (invalid RSQL expression), `fields` (invalid comma separated list of JSON field names), or `address` (invalid URL).<br/><br/> The error message returned to the caller is:<br/> "**Error creating subscription. The request has wrong parameters: {parsing exception text}**" | Review the request generating code, ensuring that a valid syntax is used for `filterCriteria`, `fields` and `address` fields. |
| **ENS-K-03**<br/> Subscription Exists | The creation of a new subscription failed because another subscription with exactly the same parameters `tenant`, `address` and `subscriptionFilter` already exists.<br/><br/> The error message returned to the caller is:<br/> "**Error creating subscription. A subscription with the same properties already exists. Subscription id: '{id of the existing subscription}'**" | This error is not necessarily a bug and could be ignored. Review the request generating code to understand why it tried to create a duplicated subscription. |
| **ENS-E-04**<br/> Database Error in Subscription Creation | There was an error creating the subscription because Notification Service failed to access the database.<br/><br/> The error message returned to the caller is:<br/> "**Error creating subscription. Database error: {exception text}**" | A transient issue with the database, for example database restart or a temporary connection problem, could cause this error; in such cases retrying the operation can solve the problem.<br/>If the problem persists, the `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-Z-05**<br/> Unexpected Error in Subscription Creation | In general this is a bug of the Notification Service. It means that an exception not foreseen and captured in the code has been thrown. <br/><br/> The error message returned to the caller is:<br/> "**Error creating subscription. An unexpected error occurred: {exception text}**" | The `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-J-06** Subscription Not Found | There was an error retrieving the subscription, because no subscription with the provided identifier could be found in the database.<br/><br/> The error message returned to the caller is:<br/> "**Error retrieving subscription. A subscription with the provided id '{requested subscription id}' couldn't be found**" | This error is not necessarily a bug and could be ignored. Review the request generating code to understand why it tried to retrieve a non-existing subscription. |
| **ENS-E-07**<br/> Database Error in Subscription Retrieval | There was an error retrieving the subscription because Notification Service failed to access the database.<br/><br/> The error message returned to the caller is:<br/> "**Error retrieving subscription. Database error: {exception text}**" | A transient issue with the database, for example database restart or a temporary connection problem, could cause this error; in such cases retrying the operation can solve the problem.<br/>If the problem persists, the `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-Z-08**<br/> Unexpected Error in Subscription Retrieval | In general this is a bug of the Notification Service. It means that an exception not foreseen and captured in the code has been thrown. <br/><br/> The error message returned to the caller is:<br/> "**Error retrieving subscription. An unexpected error occurred: {exception text}**" | The `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-J-09**<br/> Subscription Not Found | There was an error deleting the subscription, because no subscription with the provided identifier could be found in the database.<br/><br/> The error message returned to the caller is:<br/> "**Error deleting subscription. A subscription with the provided id '{requested subscription id}' couldn't be found**" | This error is not necessarily a bug and could be ignored. Review the request generating code to understand why it tried to delete a non-existing subscription. |
| **ENS-E-10**<br/> Database Error Deleting Subscription | There was an error deleteing the subscription because Notification Service failed to access the database.<br/><br/> The error message returned to the caller is:<br/> "**Error deleting subscription. Database error: {exception text}**" | A transient issue with the database, for example database restart or a temporary connection problem, could cause this error; in such cases retrying the operation can solve the problem.<br/>If the problem persists, the `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-Z-11**<br/> Unexpected Error Deleting Subscription | In general this is a bug of the Notification Service. It means that an exception not foreseen and captured in the code has been thrown. <br/><br/> The error message returned to the caller is:<br/> "**Error deleting subscription. An unexpected error occurred: {exception text}**" | The `userMessage`, a description of the error scenario and the relevant log files should be reported to the Notification Service Team for further analysis. |
| **ENS-B-12**<br/> Error Parsing Request | There was an error completing the requested operation, because a malformed parameter is present in the request.<br/> For example, a wrong UUID is used to specify a subscription in GET/DELETE requests, or JSON errors are present in CREATE requests, like missing commas or misplaced parenthesis in the body.<br/><br/> The error message returned to the caller is:<br/> "**Error parsing request: {parsing exception text}**" | This error is likely due to a bug in the caller code or in the data used by the caller code to form the request. Review that and fix the issue as appropriate. |
<p align="center"> Table 1 - Notification Service REST API Errors </p>


## <a name="expected-notifications-not-received"></a>Expected Notifications Not Received

If a client creates a subscription and does not receive notifications for it, and the client can confirm that the events matching that 
subscription are processed, an investigation should be done to understand why this issue has occurred. There are multiple possible causes for it. 
Take into account that each event received from Kafka is logged by ENS this way:

```json
{
    "timestamp":"2021-07-14T12:26:49.703Z",
    "version":"0.3.0",
    "message":"#### -> Received event -> {\"eventID\":\"1601897c-53cb-4e18-a5a8-316106ba189c\",\"eventType\":\"myType\",\"eventTime\":1626265609000,\"tenant\":\"myTenant\",\"payLoad\":\"\\\"myPayload\\\"\",\"descriptor\":\"Test message\"}",
    "logger":"com.ericsson.oss.common.service.ns.business.EventProcessor",
    "thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1",
    "service_id":"unknown",
    "correlation_id":"fd2d19bca4801973",
    "severity":"info"
}
```
The most frequent causes for not receiving a notification are:

- **The producer could not deliver the event**: Search the ENS log. If you cannot find  a log record for the event that is expected, it means that an issue happened in the producer, which should now be investigated.
- **Invalid events are received**: this should not occur if the producers who create the events use the builder that is defined in the `eric-notification-service-api`. This API populates the `NsEvent` structure correctly, with all the required mandatory parameters. 
If a vanilla producer creates and sends an invalid event, a log error similar to the following one is generated by ENS:

```json
{
    "timestamp":"2021-07-14T12:21:58.856Z",
    "version":"0.3.0",
    "message":"Invalid notification syntax",
    "logger":"com.ericsson.oss.common.service.ns.business.EventProcessor",
    "thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1",
    "service_id":"unknown",
    "correlation_id":"9df8381125ee3ae0",
    "severity":"error"
}
```

- **The event that is received does not match the subscription**: The event must match the `tenant` and at least one `eventType` of the subscription. Furthermore, if a filter is defined, it must match the event `payLoad`.
<br/><br/>
If no subscription matches, a log record similar to the following one is generated by ENS:

```json
{
    "timestamp":"2021-07-14T12:26:49.805Z",
    "version":"0.3.0",
    "message":"No subscription matches received eventType/tenant",
    "logger":"com.ericsson.oss.common.service.ns.business.EventProcessor",
    "thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1",
    "service_id":"unknown",
    "correlation_id":"fd2d19bca4801973",
    "severity":"warning"
}
```
This is not necessarily a problem, because in general it can happen that no subscriptions match a given event.
Furthermore, if at least one subscription matches, but the notification is delivered to other clients, no log is left at all. <br/><br/>
In such cases you should double-check the `eventType`, `tenant` and `filter` fields of your subscription to ensure that they allow to send the 
notification.

- **The notification cannot be sent to the client**: There are issues that can prevent a client from receiving notifications.
One is a wrong `address` specified in the subscription. For example, if a non-existent `address` URL is specified, the logs show output similar to the following: 

```json
{"timestamp":"2021-07-14T12:42:42.213Z","version":"0.3.0","message":"#### -> Received event -> {\"eventID\":\"e82d4dca-7205-44e5-8ea9-eb8c4ce30668\",\"eventType\":\"myType\",\"eventTime\":1626266562000,\"tenant\":\"myTenant\",\"payLoad\":\"\\\"myPayload\\\"\",\"descriptor\":\"Test message\"}","logger":"com.ericsson.oss.common.service.ns.business.EventProcessor","thread":"org.springframework.kafka.KafkaListenerEndpointContainer#0-0-C-1","service_id":"unknown","correlation_id":"dca39a6b8627616c","severity":"info"}
{"timestamp":"2021-07-14T12:42:42.247Z","version":"0.3.0","message":"Trying to deliver notification for event e82d4dca-7205-44e5-8ea9-eb8c4ce30668 to http://myhost2","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"info"}
{"timestamp":"2021-07-14T12:42:42.313Z","version":"0.3.0","message":"Notification failed sent to http://myhost2 with error I/O error on POST request for \"http://myhost2\": myhost2: Temporary failure in name resolution; nested exception is java.net.UnknownHostException: myhost2: Temporary failure in name resolution","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"error"}
{"timestamp":"2021-07-14T12:42:42.316Z","version":"0.3.0","message":"Retry sending to http://myhost2","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"info"}
{"timestamp":"2021-07-14T12:42:45.320Z","version":"0.3.0","message":"Notification failed sent to http://myhost2 with error I/O error on POST request for \"http://myhost2\": myhost2; nested exception is java.net.UnknownHostException: myhost2","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"error"}
{"timestamp":"2021-07-14T12:42:45.321Z","version":"0.3.0","message":"Retry sending to http://myhost2","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"info"}
{"timestamp":"2021-07-14T12:42:48.322Z","version":"0.3.0","message":"Failure dispatching notification to http://myhost2","logger":"com.ericsson.oss.common.service.ns.business.NotificationDispatcher","thread":"pool-1-thread-1","service_id":"unknown","severity":"error"}
```

In the previous example, the subscription is matching the event, but there is no client with **http://myhost2** host waiting for notifications. ENS tries to deliver the notification a number of times. If the retries are not a success, an error message is logged.
<br/>
There are many network errors that can occur that may not be related to a wrong address in the subscription; ensure to verify this. Remember that notifications are always sent as POST to the address specified in the subscription.
<br/><br/>
The body of the notification must be equal to the event `payLoad`, if it is not, there might be some fields missing. Check that the `field` attribute is specified in the matching subscription. If it is, the `fields` attribute should include all the mandatory fields expected by the notification.
<br\>
ENS is not aware of the format of the notification expected by the client and cannot check whether a mandatory field is present or not: that is a contract between the producer and the client, so the client is responsible for registering a valid subscription and providing all the required fields in the notification. 
<br/><br/>
Retransmissions are attempted only for network errors or HTTP 5xx errors. 4xx errors do not cause the notification to be retransmitted.

## <a name="unexpected-notifications-received"></a>Unexpected Notifications Received

There are two causes for receiving unexpected notifications.

- **A problem with the subscription**: if unexpected notifications are received, there could an issue with the subscription where the subscription filter has not filtered out the event. In this case you should double check the `filterCriteria` attribute of the subscription.
- **A duplicate notification is received**: in case of a restart of the Notification Service, it can happen that a notification is sent twice. 
Check the number of restarts of the Notification Service pods to verify whether this issue has occurred.

<pre>
$ kubectl get pods -n testing
NAME                                            READY   STATUS    RESTARTS   AGE
eric-data-coordinator-zk-0                      1/1     Running   0          22d
eric-data-coordinator-zk-1                      1/1     Running   0          22d
eric-data-coordinator-zk-2                      1/1     Running   0          22d
eric-data-message-bus-kf-0                      1/1     Running   0          22d
eric-data-message-bus-kf-1                      1/1     Running   4          22d
eric-data-message-bus-kf-2                      1/1     Running   0          22d
eric-oss-notification-service-8f878b976-kgc4d   1/1     Running   3          14d
eric-oss-notification-service-8f878b976-nwx9r   1/1     Running   3          14d
eric-oss-notification-service-database-pg-0     2/2     Running   11         22d
eric-pm-server-0                                3/3     Running   0          22d
</pre>

To check if the notification sent is an actual duplicate notification, therefore an unexpected notification, you should check both the number of RESTARTS and the AGE of the Notification Service pod. In the example above, there are three restarts, but the pod age is 14 days. This means that the last restart happened 14 days ago, so it is unlikely that the most recent unexpected notification is a duplicate.
<br/>
In this scenario, the client and producer code should provide a method to check for duplicates in the notification details.<br/> 
For example, perhaps use a unique sequential event number, which can be stored by the client when it is received the first time.<br/>
If the current sequence number that is received is equal or lower than the last received valid sequence number, then you can confirm that the recent notification is a duplicate, which can be ignored.
