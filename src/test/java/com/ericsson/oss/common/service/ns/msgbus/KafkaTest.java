/*******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.common.service.ns.msgbus;

import com.ericsson.oss.common.service.ns.PostgreSqlContainerBase;
import com.ericsson.oss.common.service.ns.api.helper.NsEventBuilder;
import com.ericsson.oss.common.service.ns.api.model.NsEvent;
import com.ericsson.oss.common.service.ns.filter.FilterBase;
import com.ericsson.oss.common.service.ns.model.OAuth2TokenResponse;
import com.ericsson.oss.common.service.ns.model.additional.properties.AdditionalInformation;
import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.model.subscription.SubscriptionFilter;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.common.service.ns.repository.CredentialsRepository;
import com.ericsson.oss.common.service.ns.repository.DispatchRepository;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;
import com.ericsson.oss.common.service.ns.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.ListenableFuture;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = "logging.config=classpath:logback.xml")
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@ActiveProfiles("test-kafka")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = { "spring.cloud.kubernetes.enabled = false" })
class KafkaTest extends PostgreSqlContainerBase {

    private final Logger logger = LoggerFactory.getLogger(KafkaTest.class);

    @Autowired
    private SubscriptionRepository subscriptionTable; // The database table of subscriptions

    @Autowired
    private DispatchRepository dispatchTable; // The database table of subscriptions

    @Autowired
    private KafkaTemplate<String, String> template;

    @Autowired
    private PerformanceMetrics metrics;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.kafka.event-topic}")
    private String topic;

    private MockWebServer mockWebServer;

    private static final int LONG_TIMEOUT = 10; // "Long timeout for tests
    private static final int TEST_TIMEOUT = 8; // "Normal" Timeout for tests (seconds)
    private static final int SHORT_TIMEOUT = 1; // Short timeout

    private int attemptNo; // Variable for repeating attempts in mocked client code
    private static String mockUrl;

    /**
     * Reset the counters and the database before each test
     */
    @BeforeEach
    void reset() {
        metrics.resetCounters();
    }

    /**
     * Print a string highlighting the end of the test in the log.
     */
    @AfterEach
    void closeTest() {
        logger.info("--------------- end of test ------------------------------------------------------------------------------------");
    }

    @BeforeAll
    void waitForKafka() throws IOException {
        logger.info("--------------- Waiting for Kafka------------------------------------------------------------------------------------");
        do {
            logger.info("Trying kafka template...");
            try {
                template.send(topic, "dummy message");
                break;
            } catch (NoSuchFieldError ex) {
                sleep(1000);
            }
        } while (true);
        metrics.resetCounters();
        sleep(1000);
        mockWebServer = new MockWebServer();
        mockUrl = "http://localhost:" + mockWebServer.getPort();
        logger.info("--------------- Tests start ------------------------------------------------------------------------------------");
    }

    @AfterAll
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * Wait for a given period of time.
     *
     * @param msec
     *         The time to wait in milliseconds
     */
    private void sleep(long msec) {
        try {
            TimeUnit.MILLISECONDS.sleep(msec);
        } catch (Exception ex) {
            // Do nothing
        }
    }

    /**
     * Check that the metrics have expected values
     *
     * @param eventCount
     *         Expected number of events processed
     * @param successNotif
     *         Expected number of notifications sent successfully
     * @param retriedNotif
     *         Expected number of notifications sent after retry
     * @param failNotif
     *         Expected number of failed notifications
     * @param projNotif
     *         Expected number of projected notifications
     * @param droppedNotif
     *         Expected number of dropped notifications
     * @param subscriptions
     *         Expected number of active subscriptions
     */
    private void checkCounters(int eventCount,
                               int successNotif,
                               int retriedNotif,
                               int failNotif,
                               int projNotif,
                               int droppedNotif,
                               int subscriptions) {
        sleep(1000); // Wait for micrometer to update all measures
        assertEquals(eventCount, metrics.getEventCount(), "Events count: ");
        assertEquals(successNotif, metrics.getSuccNotifCount(), "Successful notifications count: ");
        assertEquals(retriedNotif, metrics.getSuccRetriedNotifCount(), "Successful retried notifications count: ");
        assertEquals(failNotif, metrics.getFailNotifCount(), "Failed notifications count: ");
        assertEquals(projNotif, metrics.getProjNotifCount(), "Projected notifications count: ");
        assertEquals(droppedNotif, metrics.getDroppedNotifCount(), "Dropped notifications count: ");
        assertEquals(subscriptions, metrics.getActiveSubscriptionsCount(), "Current subscriptions count: ");
    }

    /**
     * Print a header with the title of the test.
     *
     * @param title
     */
    private void testHeader(String title) {
        logger.info("================================================================================================================");
        logger.info(title);
        logger.info("================================================================================================================");
    }

    private void setSubscription(String eventType, String tenant, String address, String filterCriteria, String fields) {
        Subscription s =
                new Subscription(UUID.randomUUID(),
                        tenant,
                        address,
                        Collections.singletonList(new SubscriptionFilter(nextInt(), eventType, filterCriteria, fields, hashCode())));
        subscriptionTable.save(s);
    }

    private Subscription setSubscription(String eventType, String tenant, String address, String filterCriteria, String fields, Credentials credentials) {
        Subscription s =
                new Subscription(UUID.randomUUID(),
                        tenant,
                        address,
                        Collections.singletonList(new SubscriptionFilter(nextInt(), eventType, filterCriteria, fields, hashCode())),
                        credentials);
        return subscriptionTable.save(s);
    }

    private Credentials setCredentials(String apiKey, String clientSecret, String clientId, String grantType, String authType, String tokenUrl, String authHeaders, String authTokenRequest) {
        Credentials credentials = new Credentials(UUID.randomUUID(), clientSecret, clientId, apiKey, grantType, authType,tokenUrl, authHeaders, authTokenRequest);
        return credentials;
    }

    /**
     * Consume one message. Checks that if a valid event is sent to the Kafka bus, with an active subscriber, then it's consumed by
     * the consumer and a POST-ed to the subscriber.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeSingleMessage() throws Exception {
        testHeader(
                "Test 1 (positive): when valid event is sent to kafka and an active subscription matches, then the event is delivered to the client"
                        + "specified by the subscription");
        CountDownLatch latch = new CountDownLatch(1);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        setSubscription("MyType", "me", mockUrl, null, null);
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 1);
    }

    /**
     * Sends an invalid event (wrong NotificationEvent structure). Checks that no notification arrives to the destination
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeInvalidEvent() throws Exception {
        testHeader("Test 2 (negative): when invalid event (wrong syntax) is sent to kafka no dispatching occurs");
        CountDownLatch latch = new CountDownLatch(1);
        setSubscription("MyType", "me", mockUrl, null, null);
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload("invalid").setHeader(KafkaHeaders.TOPIC, topic).build());

        checkCounters(1, 0, 0, 0, 0, 0, 1);
    }

    /**
     * Sends an invalid message (missing mandatory fields). Checks that no notification arrives to the destination
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeInvalidMessage() throws Exception {
        testHeader("Test 3 (negative): when invalid event (missing, wrong fields) is sent to kafka no dispatching occurs");
        CountDownLatch latch = new CountDownLatch(1);
        // Missing eventID
        NsEvent event = new NsEvent();
        event.setEventTime(Utils.getCurrentTimestamp());
        event.setEventType("MyType");
        event.setTenant("me");
        event.setPayLoad("\"message1\"");
        setSubscription("MyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        // Missing eventTime
        event = new NsEvent();
        event.setEventID("1");
        event.setEventType("MyType");
        event.setTenant("me");
        event.setPayLoad("\"message1\"");
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        // Missing eventType
        event = new NsEvent();
        event.setEventID("1");
        event.setEventTime(Utils.getCurrentTimestamp());
        event.setTenant("me");
        event.setPayLoad("\"message1\"");
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        // Missing tenant
        event = new NsEvent();
        event.setEventID("1");
        event.setEventTime(Utils.getCurrentTimestamp());
        event.setEventType("MyType");
        event.setPayLoad("\"message1\"");
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        // Missing payload
        event = new NsEvent();
        event.setEventID("1");
        event.setEventTime(Utils.getCurrentTimestamp());
        event.setEventType("MyType");
        event.setTenant("me");
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        // Wrong payload
        event = new NsEvent();
        event.setEventID("1");
        event.setEventTime(Utils.getCurrentTimestamp());
        event.setEventType("MyType");
        event.setTenant("me");
        event.setPayLoad("invalid");
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        checkCounters(6, 0, 0, 0, 0, 0, 1);
    }

    /**
     * Sends a valid message but the client is not listening. Checks that after the maximum retries an error message is sent to the
     * sender and the status of the subscription is put INACTIVE
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchNoClient() throws Exception {
        testHeader("Test 4 (negative): when valid event is sent to kafka but no client is listenting the notification delivery fails");
        CountDownLatch latch = new CountDownLatch(1);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        setSubscription("MyType", "me", "http://test-4/v1/notification", null, null);
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        //  use LogCaptor<NotificationDispatcher> logCaptor = LogCaptor.forClass(NotificationDispatcher.class); here to keep track of the log
        assertEquals(1L, latch.getCount());
        checkCounters(1, 0, 0, 1, 0, 0, 1);
    }

    /**
     * Send a valid message without any matching subscription for event type and tenant. No notification is sent.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchNoMatchingSubscription() throws Exception {
        testHeader("Test 5 (negative): when valid event is sent to kafka but no subscription matches no notification is sent");
        CountDownLatch latch = new CountDownLatch(1);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        setSubscription("MyType", "notme", mockUrl, null, null);
        setSubscription("NotMyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);
        //  use  LogCaptor<NotificationDispatcher> logCaptor = LogCaptor.forClass(NotificationDispatcher.class);

        checkCounters(1, 0, 0, 0, 0, 0, 2);
    }

    /**
     * Dispatch a valid message to a subscription with not matching filter. Check that no event gets dispatched to the subscriber
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchWithNotMatchingFilter() throws Exception {
        testHeader("Test 6 (negative): when valid event is sent to kafka but subscription filter doesn't match no notification is sent");
        CountDownLatch latch = new CountDownLatch(1);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        event.setPayLoad("{\"name\":\"validPayloadName\"}");
        setSubscription("MyType", "me", mockUrl, "name==notValidName", null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        checkCounters(1, 0, 0, 0, 0, 1, 1);
    }

    /**
     * Dispatch a valid message to a subscription with matching filter. Check that the event gets dispatched to the subscriber
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchWithMatchingFilter() throws Exception {
        testHeader("Test 7 (positive): when valid event is sent to kafka and subscription filter matches notification is sent to client");

        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        event.setPayLoad("{\"name\":\"validPayloadName\"}");
        setSubscription("MyType", "me", mockUrl, "name==validPayloadName", null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 1);
    }

    /**
     * When a set of subscriptions leads to duplicate ID/address only one instance of the notification is sent to the client
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchDuplicatedAddress() throws Exception {
        testHeader(
                "Test 8 (positive): when valid event is sent to kafka and more subscriptions match with same address, only one is sent to that "
                        + "address");
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        event.setPayLoad("{\"name\":\"validPayloadName\"}");
        setSubscription("MyType", "me", mockUrl, "name==validPayloadName", null);
        setSubscription("MyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 2);
    }

    /**
     * Dispatch a valid message to a subscription with matching filter and a projection. Check that the event gets dispatched to the subscriber with
     * projected payload
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchWithMatchingFilterAndProjection() throws Exception {
        testHeader(
                "Test 9 (positive): when valid event is sent to kafka and subscription has \"fields\" set, the notification is sent to the client "
                        + "with only the specified fields");
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        event.setPayLoad("{\"id\":1, \"name\":\"validPayloadName\", \"status\": \"active\"}");
        setSubscription("MyType", "me", mockUrl, "name==validPayloadName", "id,status");

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 1, 0, 1);
    }

    /**
     * Consume more than one message. Checks that if a number of valid events are published to the Kafka bus, with an active
     * subscriber, then they are consumed by the consumer and POST-ed to the relevant subscribers.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeMultipleMessages() throws Exception {
        testHeader("Test 10 (positive): when 2 events  sent to kafka and subscriptions match the notifications are sent to the clients");
        CountDownLatch latch = new CountDownLatch(3);
        MockWebServer mockWebServer = new MockWebServer();
        String mockUrl = "http://localhost:" + mockWebServer.getPort();
        NsEvent event1 = NsEventBuilder.build("MyFirstType", "me", "message1");
        event1.setPayLoad("{\"id\":1, \"name\":\"validPayloadName\", \"status\": \"active\"}");
        NsEvent event2 = NsEventBuilder.build("MySecondType", "me", "message1");
        event2.setPayLoad("{\"name\":\"validPayloadName\"}");

        setSubscription("MyFirstType", "me", mockUrl + "/test-10", null, "id,name");
        setSubscription("MyFirstType", "me", mockUrl + "/test-10b", "name==validPayloadName", null);
        setSubscription("MySecondType", "me", mockUrl + "/test-10b", "name==notValidPayloadName", null);
        setSubscription("MySecondType", "me", mockUrl + "/test-10b", null, null);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/test-10")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-10b")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        mockWebServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event1)).setHeader(KafkaHeaders.TOPIC, topic).build());
        template.send(MessageBuilder.withPayload(Utils.serialize(event2)).setHeader(KafkaHeaders.TOPIC, topic).build());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getBody().toString().contains("validPayloadName"));

        checkCounters(2, 3, 0, 0, 1, 1, 4);
    }

    /**
     * Check that pending events read from the database take precedence over incoming event (if the id matches).
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumePendingNotifications() throws Exception {
        testHeader(
                "Test 11 (positive): when an event arrive with record for its eventID in database, the pending notifications are sent to the "
                        + "clients");
        NsEvent event1 = NsEventBuilder.build("MyFirstType", "me", "message1");
        event1.setPayLoad("{\"id\":1, \"name\":\"validPayloadName\", \"status\": \"active\"}");
        dispatchTable.save(new Dispatch(event1.getEventID(), mockUrl, 1, "\"pending-message1\"", null, "application/json", null));
        dispatchTable.save(new Dispatch(event1.getEventID(), mockUrl, 2, "\"pending-message2\"", null, "application/json", null));
        setSubscription("MyFirstType", "me", mockUrl, null, "id,name");
        setSubscription("MyFirstType", "me", mockUrl, null, null);
        setSubscription("MySecondType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        template.send(MessageBuilder.withPayload(Utils.serialize(event1)).setHeader(KafkaHeaders.TOPIC, topic).build());
        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 2, 0, 0, 0, 0, 3);
    }

    /**
     * When a set of subscriptions leads to duplicate ID/address only one instance of the notification is sent to the client
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchSuccessfulRetry() throws Exception {
        testHeader(
                "Test 12 (positive): when a valid event is sent to kafka and client refuse the first delivery, then retry and second time is "
                        + "successful");
        MockWebServer mockWebServer = new MockWebServer();
        String mockUrl = "http://localhost:" + mockWebServer.getPort();
        CountDownLatch latch = new CountDownLatch(1);
        attemptNo = 0;
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1");
        event.setPayLoad("{\"name\":\"validPayloadName\"}");
        setSubscription("MyType", "me", mockUrl + "/test-12", "name==validPayloadName", null);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/test-12") && attemptNo == 0) {
                    attemptNo++;
                    return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                } else if (recordedRequest.getPath().equals("/test-12")) {
                    return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        mockWebServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        latch.await(TEST_TIMEOUT, TimeUnit.SECONDS);

        checkCounters(1, 0, 1, 0, 0, 0, 1);
        mockWebServer.shutdown();
    }

    /**
     * Test real scenario where a TMF event matches a number of subscriptions/filters.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void dispatchRealEvent() throws Exception {
        testHeader("Test 13 (real scenario): a real TMF event matches a number of sucscriptions");
        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();
        CountDownLatch latch = new CountDownLatch(11);
        attemptNo = 0;
        NsEvent event = NsEventBuilder.build("ServiceOrderCreateEvent", "me", "message1");
        event.setPayLoad(new FilterBase().getServiceOrderCreateEvent());
        // Create 11 subscriptions to different clients. 7 of them match the event, 3 don't, and 1 matches but no client mock is provided for it
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13", null, null);
        setSubscription("ServiceOrderCreateEvent", "you", mockUrl + "/test-13", null, null);
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13b", "event.serviceOrder.state==acknowledged", null);
        setSubscription("ServiceOrderCreateEvent",
                "me",
                mockUrl + "/test-13c",
                "event.serviceOrder.state==acknowledged;event.serviceOrder.completionDate!=\"\"",
                null);
        setSubscription("ServiceOrderCreateEvent",
                "me",
                mockUrl + "/test-13d",
                "event.serviceOrder.state==acknowledged;event.serviceOrder.completionDate==\"\"",
                null);
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13e", "event.serviceOrder.note[0].author=='Harvey Poupon'", null);
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13f", "event.serviceOrder.note[0].author==invalid", null);
        setSubscription("ServiceOrderCreateEvent",
                "me",
                mockUrl + "/test-13g",
                "event.serviceOrder.state==acknowledged,event.serviceOrder.note[0].author==invalid",
                "eventId");
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13h", null, "eventId,eventTime,eventType");
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13i", null, "event.serviceOrder.id,event.serviceOrder.state");
        setSubscription("ServiceOrderCreateEvent", "me", mockUrl + "/test-13j", null, null); // client no answer

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/test-13")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13b")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13c")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13d")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13e")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13f")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13g")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13h")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                if (recordedRequest.getPath().equals("/test-13i")) {
                    return new MockResponse().setResponseCode(HttpStatus.OK.value());
                }
                return new MockResponse().setResponseCode(500);
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        // Wait for procedure completions
        latch.await(LONG_TIMEOUT, TimeUnit.SECONDS);
        // 4 notifications are not received
        checkCounters(1, 7, 0, 1, 3, 2, 11);
        logger.info("Event management time: {} msec", metrics.getEventManagementTimeValue());
        // Check the mean processing time is less than maximum rate specified in PSS (1 event every 30 sec)
        assertTrue(metrics.getEventManagementTimeValue() < 30000);
        webServer.shutdown();
    }

    /**
     * Consume one message. Checks that if a valid event is sent to the Kafka bus along with headers information, with an active subscriber, then it's consumed by
     * the consumer and a POST-ed to the subscriber.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeNotificationWithAdditionalHeaders() throws Exception {
        testHeader("Test 14 (positive): when valid event is sent to kafka along with headers information and an active subscription matches, then the event is delivered to the client"
                + "specified by the subscription");
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, String> headers = new HashMap<>();
        AdditionalInformation additionalInformation = new AdditionalInformation();
        headers.put("X-Group-ID","B2BSmallandMediumBusiness");
        headers.put("X-Correlation-ID","5293fcac-d423-4d59-a292-b3364915dd59");
        additionalInformation.setHeaders(headers);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1", "", additionalInformation);
        setSubscription("MyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 1);
    }

    /**
     * Consume one message. Checks that if a valid event is sent to the Kafka bus along with empty headers information, with an active subscriber, then it's consumed by
     * the consumer and a POST-ed to the subscriber.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeNotificationWithEmptyAdditionalHeaders() throws Exception {
        testHeader("Test 15 (negative): when valid event is sent to kafka along with empty headers information and an active subscription matches, then the event is delivered to the client"
                + "specified by the subscription");
        CountDownLatch latch = new CountDownLatch(1);
        AdditionalInformation additionalInformation = new AdditionalInformation();
        Map<String, String> headers = new HashMap<>();
        additionalInformation.setHeaders(headers);
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1", "", additionalInformation);

        setSubscription("MyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 1);
    }

    /**
     * Consume one message. Checks that if a valid event is sent to the Kafka bus along with missing headers information, with an active subscriber, then it's consumed by
     * the consumer and a POST-ed to the subscriber.
     *
     * @throws Exception
     */
    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeNotificationWithInvalidAdditionalHeaders() throws Exception {
        testHeader("Test 16 (negative): when valid event is sent to kafka along with missing headers information and an active subscription matches, then the event is delivered to the client"
                + "specified by the subscription");
        CountDownLatch latch = new CountDownLatch(1);
        AdditionalInformation additionalInformation = new AdditionalInformation();
        NsEvent event = NsEventBuilder.build("MyType", "me", "message1", "", additionalInformation);

        setSubscription("MyType", "me", mockUrl, null, null);

        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());
        RecordedRequest recordedRequest = mockWebServer.takeRequest(TEST_TIMEOUT, TimeUnit.SECONDS);
        assertEquals("POST", recordedRequest.getMethod());
        checkCounters(1, 1, 0, 0, 0, 0, 1);
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2Notifications() throws InterruptedException, IOException {
        testHeader("Test 17 (positive): when valid event is sent to kafka for an OAuth2 subscription, then the event is delivered to the client specified by the subscription");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b2", "password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "{\"Content-Type\":\"application/json\"}", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 1, 0, 0, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2PendingNotifications() throws Exception {
        testHeader(
                "Test 18 (positive): when an event arrive with record for its eventID in database for OAuth2, the pending notifications are sent to the clients");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b2", "password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");
        dispatchTable.save(new Dispatch(event.getEventID(), mockUrl + "/v1/notification", 1, "\"pending-message1\"", credentials.getId().toString(), "{\"Content-Type\":\"application/json\"}", null));

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 0, 0, 1, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2NotificationsWithInvalidAccessToken() throws InterruptedException, IOException {
        testHeader("Test 19 (Negative): when valid event is sent to kafka for an OAuth2 subscription with invalid client credentials, then the event is rejected");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b2", "invalid-password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "{\"Content-Type\":\"application/json\"}", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 0, 0, 1, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2NotificationsURLEncoded() throws InterruptedException, IOException {
        testHeader("Test 20 (positive): when valid event is sent from kafka for an OAuth2 subscription, then the event is delivered to the client specified by the subscription, using encoded URL");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b9", "password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "{\"Content-Type\":\"application/x-www-form-urlencoded\"}", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 1, 0, 0, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2PendingNotificationsURLEncoded() throws Exception {
        testHeader(
                "Test 21 (positive): when an event arrive with record for its eventID in database for OAuth2, the pending notifications are sent to the clients");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b2", "password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");
        dispatchTable.save(new Dispatch(event.getEventID(), mockUrl + "/v1/notification", 1, "\"pending-message1\"", credentials.getId().toString(), "{\"Content-Type\":\"application/x-www-form-urlencoded\"}", null));

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 0, 0, 1, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2NotificationsWithInvalidAccessTokenURLEncoded() throws InterruptedException, IOException {
        testHeader("Test 22 (Negative): when valid event is sent from kafka for an OAuth2 subscription with invalid client credentials, then the event is rejected, using encoded URL");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b1", "invalid-password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "{\"Content-Type\":\"application/x-www-form-urlencoded\"}", null);
        setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(2000);
        checkCounters(1, 0, 0, 1, 0, 0, 1);
        webServer.shutdown();

    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2NotificationsJsonAuth() throws InterruptedException, IOException {
        testHeader("Test 23 (positive): when valid event is sent from kafka for an OAuth2 subscription with json token request, then the event is delivered to the client specified by the subscription");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b9", "password", "client", "client_credentials", "oauth2", mockUrl + "/v1/notifications/token", "{\"Content-Type\":\"application/json\"}", "json");
        setSubscription("MyType", "tenant", mockUrl + "/v1/notifications/token/endpoint", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/v1/notifications/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notifications/token/endpoint")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(4000);
        checkCounters(1, 1, 0, 0, 0, 0, 1);
        webServer.shutdown();
    }

    @Test
    @Sql("/db/clear-subscriptions.sql")
    void consumeOAuth2PendingNotificationsJsonAuth() throws Exception {
        testHeader(
                "Test 24 (positive): when an event arrive with record for its eventID in database for OAuth2 with json token request, the pending notifications are sent to the clients");

        MockWebServer webServer = new MockWebServer();
        String mockUrl = "http://localhost:" + webServer.getPort();

        Credentials credentials = setCredentials("1b8a7758-5f1e-4973-bc09-b1f9f5f511b2", "password", "client", "client_credentials", "oauth2", mockUrl + "/oauth/token", "", "json");
        Subscription subscription = setSubscription("MyType", "tenant", mockUrl + "/v1/notification", null, null, credentials);
        NsEvent event = NsEventBuilder.build("MyType", "tenant", "message1");
        dispatchTable.save(new Dispatch(event.getEventID(), mockUrl + "/v1/notification", 1, "\"pending-message1\"", subscription.getCredentials().getId().toString(), "{\"Content-Type\":\"application/json\"}", null));

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse("4222d769-f9d0-409a-8386-0fb60c5e1b3e", "Bearer");
        String accessTokenResponse1 = objectMapper.writeValueAsString(oAuth2TokenResponse);

        Dispatcher dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                if (recordedRequest.getPath().equals("/oauth/token")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody(accessTokenResponse1);
                }
                if (recordedRequest.getPath().equals("/v1/notification")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        };
        webServer.setDispatcher(dispatcher);

        template.send(MessageBuilder.withPayload(Utils.serialize(event)).setHeader(KafkaHeaders.TOPIC, topic).build());

        Thread.sleep(4000);
        checkCounters(1, 0, 0, 1, 0, 0, 1);
        webServer.shutdown();
    }
}
