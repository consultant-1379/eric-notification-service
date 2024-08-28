/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.common.service.ns.business.credentialmanager;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class CredentialsManagerServiceImplTest {

    private int mockServerPort = 8090;

    @InjectMocks
    private CredentialsManagerServiceImpl credentialsManagerManagerService;

    private MockWebServer credentialsManagerMockServer;

    private final Map<String, String> responseMap = new HashMap<>();

    private static final String ENCRYPTED_DATA = "VEVTVA==";
    private static final String DECRYPTED_DATA = "Test";


    @Before
    public void setMockValueAndServer() {
        credentialsManagerMockServer = new MockWebServer();
        ReflectionTestUtils.setField(credentialsManagerManagerService, "esoSecurityHost", "http://localhost:8090");
        ReflectionTestUtils.setField(credentialsManagerManagerService, "selfServiceURL", "/v1.0/credentialsmanager/selfservice");
        ReflectionTestUtils.setField(credentialsManagerManagerService, "srmURL", "/credentialsmanager/v1.0/srm");
        ReflectionTestUtils.setField(credentialsManagerManagerService, "retryAttempts", 3);
        ReflectionTestUtils.setField(credentialsManagerManagerService, "delay", 2);
    }

    @After
    public void resetAfter() throws IOException {
        credentialsManagerMockServer.shutdown();
    }


    public static MockResponse createMockResponse(final HttpStatus httpStatus, String body) {
        return new MockResponse()
                .setResponseCode(httpStatus.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    @Test
    public void encryptData() throws IOException {
        responseMap.put("connectionProperties", ENCRYPTED_DATA);
        credentialsManagerMockServer.start(mockServerPort);
        credentialsManagerMockServer.enqueue(createMockResponse(HttpStatus.OK, new ObjectMapper().writeValueAsString(responseMap)));
        String encryptedString = credentialsManagerManagerService.encryptData(DECRYPTED_DATA);
        assertEquals(ENCRYPTED_DATA, encryptedString);
    }

    @Test
    public void decryptData() throws IOException {
        responseMap.put("connectionProperties", DECRYPTED_DATA);
        credentialsManagerMockServer.start(mockServerPort);
        credentialsManagerMockServer.enqueue(createMockResponse(HttpStatus.OK, new ObjectMapper().writeValueAsString(responseMap)));
        String decryptedString = credentialsManagerManagerService.decryptData(ENCRYPTED_DATA);
        assertEquals(DECRYPTED_DATA, decryptedString);
    }

}