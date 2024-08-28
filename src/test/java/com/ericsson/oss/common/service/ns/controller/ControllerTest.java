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

package com.ericsson.oss.common.service.ns.controller;

import com.ericsson.oss.common.service.ns.PostgreSqlContainerBase;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionFilter;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.controller.subscription.SubscriptionApiControllerImpl;
import com.ericsson.oss.common.service.ns.infrastructure.configurations.WebClientConfig;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;
import com.ericsson.oss.common.service.ns.util.Utils;
import com.ericsson.oss.orchestration.so.common.error.message.factory.message.ErrorMessage;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = { "spring.cloud.kubernetes.enabled = false" })
class ControllerTest extends PostgreSqlContainerBase {

    private static Logger logger = LoggerFactory.getLogger(ControllerTest.class);

    @LocalServerPort
    private int port;

    private int mockServerPort = 8081;

    @Autowired
    private SubscriptionApiControllerImpl controller;

    @SpyBean
    private SubscriptionRepository subscriptionTable; // The database table of subscriptions

    @Spy
    private WebClient webClient;

    @Autowired
    private WebClientConfig webClientConfig;

    @Autowired
    private PerformanceMetrics metrics;

    private MockWebServer connectedSystemsMockWebServer;
    private static String connectedSystemsResponse;
    private static String missingPropertiesResponse;
    private static String missingAuthTypeResponse;
    private static String missingClientSecret;
    private static String invalidTokenUrlResponse;
    private static String multipleConnectionsResponse;
    private static String noConnectionsResponse;
    private static String wrongAuthTypeResponse;
    private static String urlEncoding;
    private static String missingAuthHeaderNull;
    private static String missingAuthHeader_Empty;

    private final String HEADERS_ERROR_MESSAGE = "Error creating subscription. The request has incorrect http headers: ";
    private final String UNKNOWN_ERROR_MESSAGE = "Error creating subscription. An unexpected error occurred: ";
    private final String MISSING_ERROR_MESSAGE = "Error creating subscription. The request has missing mandatory fields: ";
    private final String WRONG_ERROR_MESSAGE = "Error creating subscription. The request has wrong parameters: ";
    private final String TEST_API_KEY = "MTdkZDIxMDItZDA1Ni0xMWVjLTlkNjQtMDI0MmFjMTIwMDAy";

    @BeforeAll
    public static void setup() {
        try {
            connectedSystemsResponse = Resources.toString(Resources.getResource("subscription/connectedSystemsResponse.json"), Charsets.UTF_8);
            missingPropertiesResponse = Resources.toString(Resources.getResource("subscription/missingOauthProperties.json"), Charsets.UTF_8);
            missingAuthTypeResponse = Resources.toString(Resources.getResource("subscription/missingAuthTypeProperty.json"), Charsets.UTF_8);
            invalidTokenUrlResponse = Resources.toString(Resources.getResource("subscription/invalidHttpsTokenUrl.json"), Charsets.UTF_8);
            missingClientSecret = Resources.toString(Resources.getResource("subscription/missingClientSecretProperty.json"), Charsets.UTF_8);
            noConnectionsResponse = Resources.toString(Resources.getResource("subscription/missingConnectionProperties.json"), Charsets.UTF_8);
            multipleConnectionsResponse = Resources.toString(Resources.getResource("subscription/multipleConnectionProperties.json"), Charsets.UTF_8);
            wrongAuthTypeResponse = Resources.toString(Resources.getResource("subscription/wrongAuthType.json"), Charsets.UTF_8);
            urlEncoding = Resources.toString(Resources.getResource("subscription/urlEncoding.json"), Charsets.UTF_8);
            missingAuthHeaderNull = Resources.toString(Resources.getResource("subscription/missingAuthHeaderNull.json"), Charsets.UTF_8);
            missingAuthHeader_Empty = Resources.toString(Resources.getResource("subscription/missingAuthHeader_Empty.json"), Charsets.UTF_8);
        } catch (IOException e) {
            logger.info("Setup failed {}", e.getMessage());
        }
    }

    @AfterEach
    void resetAfter() throws IOException {
        Mockito.reset(subscriptionTable);
        connectedSystemsMockWebServer.shutdown();
    }

    @BeforeEach
    void reset() {
        List<Subscription> subscriptions = subscriptionTable.findAll();
        for (Subscription s : subscriptions) {
            subscriptionTable.delete(s);
        }
        webClient = webClientConfig.createWebClient();
        connectedSystemsMockWebServer = new MockWebServer();
    }

    private NsSubscriptionRequest subscriptionReq(String eventType, String tenant, String address, String filterCriteria, String fields) {
        NsSubscriptionRequest ret = new NsSubscriptionRequest();
        if (address != null) {
            ret.setAddress(address);
        }
        if (tenant != null) {
            ret.setTenant(tenant);
        }
        List<NsSubscriptionFilter> subscriptionFilter = new ArrayList<>();
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        if (eventType != null) {
            filter.setEventType(eventType);
        }
        if (fields != null) {
            filter.setFields(fields);
        }
        if (filterCriteria != null) {
            filter.setFilterCriteria(filterCriteria);
        }
        subscriptionFilter.add(filter);
        ret.setSubscriptionFilter(subscriptionFilter);
        return ret;
    }

    public ResponseEntity sendPostRequest(NsSubscriptionRequest req) {
        ResponseEntity<NsSubscriptionResponse> res = webClient.post()
                .uri("http://localhost:" + port + "/notification/v1/subscriptions")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_JSON))
                .bodyValue(req)
                .retrieve()
                .toEntity(NsSubscriptionResponse.class)
                .block();
        return res;
    }

    public ResponseEntity<NsSubscriptionResponse> sendPostRequestWithHeaders(NsSubscriptionRequest req, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<NsSubscriptionResponse> res = webClient.post()
                .uri("http://localhost:" + port + "/notification/v1/subscriptions")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(req)
                .retrieve()
                .toEntity(NsSubscriptionResponse.class)
                .block();
        return res;
    }

    public ResponseEntity sendGetRequest() {
        final HttpHeaders headers = new HttpHeaders();
        final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<List> ret = webClient.get().uri("http://localhost:" + port + "/notification/v1/subscriptions/")
                .retrieve()
                .toEntity(List.class)
                .block();
        return ret;
    }

    public ResponseEntity sendGetRequest(String id) {
        final HttpHeaders headers = new HttpHeaders();
        final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<NsSubscriptionResponse> ret = webClient.get().uri("http://localhost:" + port + "/notification/v1/subscriptions/" + id)
                .retrieve()
                .toEntity(NsSubscriptionResponse.class)
                .block();
        return ret;
    }

    public ResponseEntity sendDeleteRequest(String id) {
        final HttpHeaders headers = new HttpHeaders();
        final HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Void> ret = webClient.delete().uri("http://localhost:" + port + "/notification/v1/subscriptions/" + id)
                .retrieve()
                .toBodilessEntity()
                .block();
        return ret;
    }

    public static MockResponse createMockResponse(final HttpStatus httpStatus, String body) {
        return new MockResponse()
                .setResponseCode(httpStatus.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    @Test
    void createSubscription_success_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
    }

    @Test
    void createSubscription_missingMandatoryField_Test() {
        ResponseEntity<NsSubscriptionResponse> resp = null;
        NsSubscriptionRequest req = subscriptionReq(null, "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-B-00", err.getErrorCode());
        }
        req = subscriptionReq("Type1", null, "http://target/v1/notification", "a==1 and b==2", "a,b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-B-00", err.getErrorCode());
        }
        req = subscriptionReq("Type1", "Tenant1", null, "a==1 and b==2", "a,b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-B-00", err.getErrorCode());
        }
    }

    @Test
    void createSubscription_wrongField_Test() {
        ResponseEntity<NsSubscriptionResponse> resp = null;
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http::::?//", "a==1 and b==2", "a,b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-C-02", err.getErrorCode());
        }
        req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a=1 and b==2", "a,b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-C-02", err.getErrorCode());
        }
        req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a;b");
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-C-02", err.getErrorCode());
        }
    }

    @Test
    void createSubscription_duplicateSubscription_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertEquals(201, resp.getStatusCodeValue());
        try {
            resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(409, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-K-03", err.getErrorCode());
        }
    }

    @Test
    void createSubscription_databaseError_Test() {
        Mockito.doThrow(new QueryTimeoutException("Database access error")).when(subscriptionTable).save(Mockito.any(Subscription.class));
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        try {
            ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-E-04", err.getErrorCode());
        }
    }

    @Test
    void createSubscription_otherError_Test() {
        Mockito.doThrow(new RuntimeException("Another runtime error")).when(subscriptionTable).save(Mockito.any(Subscription.class));
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        try {
            ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-Z-05", err.getErrorCode());
        }
    }

    @Test
    void getSubscription_success_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        resp = sendGetRequest(resp.getBody().getId().toString());
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        ResponseEntity<List<NsSubscriptionResponse>> resp2 = sendGetRequest();
        assertNotNull(resp2);
        assertEquals(200, resp2.getStatusCodeValue());
        assertEquals(1, resp2.getBody().size());
    }

    @Test
    void getSubscription_notFound_Test() {
        try {
            ResponseEntity<NsSubscriptionResponse> resp = sendGetRequest("e145d91e-eb1d-44d0-9141-ec753f5e6de6");
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(404, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-J-06", err.getErrorCode());
        }
    }

    @Test
    void getSubscription_databaseError_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        Mockito.doThrow(new QueryTimeoutException("Database access error")).when(subscriptionTable).findById(Mockito.any(UUID.class));
        try {
            resp = sendGetRequest(resp.getBody().getId().toString());
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-E-07", err.getErrorCode());
        }
    }

    @Test
    void getSubscription_otherError_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        Mockito.doThrow(new RuntimeException("Another runtime error")).when(subscriptionTable).findById(Mockito.any(UUID.class));
        try {
            resp = sendGetRequest(resp.getBody().getId().toString());
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-Z-08", err.getErrorCode());
        }
    }

    @Test
    void deleteSubscription_success_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        String id = resp.getBody().getId().toString();
        resp = sendGetRequest(id);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        resp = sendDeleteRequest(id);
        assertNotNull(resp);
        assertEquals(204, resp.getStatusCodeValue());
        try {
            resp = sendGetRequest(id);
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(404, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-J-06", err.getErrorCode());
        }
    }

    @Test
    void deleteSubscription_notFound_Test() {
        try {
            ResponseEntity<Void> resp = sendDeleteRequest("e145d91e-eb1d-44d0-9141-ec753f5e6de6");
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(404, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-J-09", err.getErrorCode());
        }
    }

    @Test
    void deleteSubscription_databaseError_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        String id = resp.getBody().getId().toString();
        resp = sendGetRequest(id);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        Mockito.doThrow(new QueryTimeoutException("Database access error")).when(subscriptionTable).delete(Mockito.any(Subscription.class));
        try {
            resp = sendDeleteRequest(id);
            assertEquals(204, resp.getStatusCodeValue());
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-E-10", err.getErrorCode());
        }
    }

    @Test
    void deleteSubscription_otherError_Test() {
        NsSubscriptionRequest req = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b");
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequest(req);
        assertNotNull(resp);
        assertEquals(201, resp.getStatusCodeValue());
        String id = resp.getBody().getId().toString();
        resp = sendGetRequest(id);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        Mockito.doThrow(new RuntimeException("Another error")).when(subscriptionTable).delete(Mockito.any(Subscription.class));
        try {
            resp = sendDeleteRequest(id);
            assertEquals(204, resp.getStatusCodeValue());
        } catch (WebClientResponseException ex) {
            assertEquals(500, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-Z-11", err.getErrorCode());
        }
    }

    @Test
    void getDeleteSubscription_wrongPath_Test() {
        try {
            ResponseEntity<Void> resp = sendGetRequest("e14-1-1---");
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-B-12", err.getErrorCode());
        }

        try {
            ResponseEntity<Void> resp = sendDeleteRequest("e14");
            assertNull(resp);
        } catch (WebClientResponseException ex) {
            assertEquals(400, ex.getRawStatusCode());
            ErrorMessage err = Utils.deserialize(ex.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals("ENS-B-12", err.getErrorCode());
        }
    }

    @Test
    void createSubscriptionWithApiKeyTest() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, connectedSystemsResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        ResponseEntity<NsSubscriptionResponse> resp = sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        assertNotNull(resp);
        assertEquals(HttpStatus.CREATED.value(), resp.getStatusCodeValue());
    }

    @Test
    void createSubscriptionWithBlankApiKeyTest() {
        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "");
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-02", err.getErrorCode());
            assertEquals(HEADERS_ERROR_MESSAGE + "X-API-KEY header was set but is blank", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionWithInvalidApiKeyTest() {
        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "123456789");
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-02", err.getErrorCode());
            assertEquals(HEADERS_ERROR_MESSAGE + "X-API-KEY header is not in base64 format", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionWithInvalidProtocolTest() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, invalidTokenUrlResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, wcre.getStatusCode());
            assertEquals("ENS-Z-05", err.getErrorCode());
            assertEquals(UNKNOWN_ERROR_MESSAGE + "The notification address is not secured under TLS protocol 'http://target/v1/notification'. This must contain https protocol for oauth2", err.getUserMessage());
        }

        nsSubscriptionRequest.setAddress("https://target/v1/notification");
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, wcre.getStatusCode());
            assertEquals("ENS-Z-05", err.getErrorCode());
            assertEquals(UNKNOWN_ERROR_MESSAGE + "The token url is not secured under TLS protocol 'http://test.com/auth/v1/token'. This must contain https protocol for oauth2", err.getUserMessage());
        }

    }

    @Test
    void createSubscriptionWithMissingOauthPropertiesTest() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, missingPropertiesResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, missingAuthTypeResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, missingClientSecret));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter fields required for oauth2 are empty or missing: [client_id, client_secret, auth_headers, grant_type, auth_url]", err.getUserMessage());
        }

        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter AUTH_TYPE value 'null' is null or not supported. Only Oauth2 or Oauth2.0 values are supported", err.getUserMessage());
        }

        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);;
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter fields required for oauth2 are empty or missing: [client_secret]", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionWithWrongAuthType() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, wrongAuthTypeResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);

        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter AUTH_TYPE value 'MyOwnType' is null or not supported. Only Oauth2 or Oauth2.0 values are supported", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionMultipleConnectionProperties() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, multipleConnectionsResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);

        ResponseEntity<NsSubscriptionResponse> nsSubscriptionResponseResponseEntity = sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        assertEquals(HttpStatus.CREATED, nsSubscriptionResponseResponseEntity.getStatusCode());
    }

    @Test
    void createSubscriptionURLEncodingConnectionProperty() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, urlEncoding));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);

        ResponseEntity<NsSubscriptionResponse> nsSubscriptionResponseResponseEntity = sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        assertEquals(HttpStatus.CREATED, nsSubscriptionResponseResponseEntity.getStatusCode());
    }

    @Test
    void createSubscriptionWithNoConnectionProperties() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, noConnectionsResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-C-02", err.getErrorCode());
            assertEquals(WRONG_ERROR_MESSAGE + "no connections properties found for API KEY: MTdkZDIxMDItZDA1Ni0xMWVjLTlkNjQtMDI0MmFjMTIwMDAy", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionAndWithRetry() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.SERVICE_UNAVAILABLE, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.REQUEST_TIMEOUT, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, connectedSystemsResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);

        ResponseEntity<NsSubscriptionResponse> response = sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        assertNotNull(response);
        assertEquals(4,connectedSystemsMockWebServer.getRequestCount());
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCodeValue());
    }

    @Test
    void createSubscriptionAndFailWithRetry() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.SERVICE_UNAVAILABLE, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.REQUEST_TIMEOUT, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, connectedSystemsResponse));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.SERVICE_UNAVAILABLE, connectedSystemsResponse));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "https://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(4, connectedSystemsMockWebServer.getRequestCount());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, wcre.getStatusCode());
            assertEquals("ENS-Z-05", err.getErrorCode());
            assertEquals("Error creating subscription. An unexpected error occurred: Retries exhausted: 3/3", err.getUserMessage());
        }
    }

    @Test
    void createSubscriptionWithMissingAuthHeadersTest() throws IOException {
        connectedSystemsMockWebServer.start(mockServerPort);
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, missingAuthHeader_Empty));
        connectedSystemsMockWebServer.enqueue(createMockResponse(HttpStatus.OK, missingAuthHeaderNull));

        NsSubscriptionRequest nsSubscriptionRequest = subscriptionReq("Type1", "Tenant1", "http://target/v1/notification", "a==1 and b==2", "a,b[*].c");
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", TEST_API_KEY);
        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter fields required for oauth2 are empty or missing: [auth_headers]", err.getUserMessage());
        }

        try {
            sendPostRequestWithHeaders(nsSubscriptionRequest, headers);
        } catch (WebClientResponseException wcre) {
            ErrorMessage err = Utils.deserialize(wcre.getResponseBodyAsString(), ErrorMessage.class);
            assertEquals(HttpStatus.BAD_REQUEST, wcre.getStatusCode());
            assertEquals("ENS-B-00", err.getErrorCode());
            assertEquals(MISSING_ERROR_MESSAGE + "Missing mandatory parameter fields required for oauth2 are empty or missing: [auth_headers]", err.getUserMessage());
        }
    }

}