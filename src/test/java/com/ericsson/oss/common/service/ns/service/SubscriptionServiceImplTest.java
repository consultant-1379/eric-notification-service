/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.common.service.ns.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.oss.common.service.ns.PostgreSqlContainerBase;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionFilter;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.exception.DuplicateSubscriptionException;
import com.ericsson.oss.common.service.ns.exception.MissingArgumentException;
import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;


@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.cloud.kubernetes.enabled = false" })
class SubscriptionServiceImplTest extends PostgreSqlContainerBase {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository repository;

    @Test
    void saveValidSubscriptionTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        NsSubscriptionResponse response = subscriptionService.save(request);
        assertNotNull(response);
        UUID id = response.getId();
        assertEquals(response, subscriptionService.get(id));
    }

    @Test
    void saveDuplicateTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setAddress("http://duplicate.com");
        NsSubscriptionFilter filter1 = new NsSubscriptionFilter();
        filter1.setEventType("OrderCompleted");
        NsSubscriptionFilter filter2 = new NsSubscriptionFilter();
        filter2.setEventType("OrderDeleted");
        request.setSubscriptionFilter(List.of(filter1, filter2));
        request.setTenant("me");
        //save first entry
        NsSubscriptionResponse response = subscriptionService.save(request);
        assertNotNull(response);
        //attempt to save entry again
        assertThrows(DuplicateSubscriptionException.class, () -> subscriptionService.save(request),
                     "A subscription with the same set of params already exists. Conflicted subscription id='" + response.getId() + "'");
    }

    @Test
    void deleteTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-delete.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        NsSubscriptionResponse subscription = subscriptionService.save(request);
        assertNotNull(subscription);
        UUID uuid = subscription.getId();
        subscriptionService.delete(uuid);
        assertThrows(NoSuchElementException.class, () -> subscriptionService.get(uuid));
    }

    @Test
    void getExistingIdTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-get-id.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        NsSubscriptionResponse subscription = subscriptionService.save(request);
        assertNotNull(subscription);
        UUID uuid = subscription.getId();
        assertNotNull(subscriptionService.get(uuid));
        assertEquals(subscription, subscriptionService.get(uuid));
    }

    @Test
    void getAllIdsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-get-all.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        NsSubscriptionResponse subscription = subscriptionService.save(request);
        List<NsSubscriptionResponse> allSubscriptions = subscriptionService.getAll();
        assertNotNull(allSubscriptions);
        assertThat(allSubscriptions.contains(subscription)).isTrue();
    }

    @Test
    void saveValidSubscriptionWithCredentialsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-creds.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));

        CredentialsInfo creds = new CredentialsInfo();
        creds.setClientId("clientId");
        creds.setTokenUrl("tokenUrl");
        creds.setClientSecret("clientSecret");
        creds.setGrantType("grantType");
        creds.setAuthType("OAuth2");
        creds.setApiKey("api-key-test");
        creds.setAuthHeaders("authHeaders");
        NsSubscriptionResponse response = subscriptionService.save(request, creds);
        assertNotNull(response);
        UUID id = response.getId();
        assertEquals(response, subscriptionService.get(id));

        Credentials credentials = repository.findById(id).get().getCredentials();
        assertNotNull(credentials);
        assertEquals(creds.getTokenUrl(), credentials.getTokenUrl());
        assertEquals(creds.getApiKey(), credentials.getApiKey());
        assertEquals(creds.getGrantType(), credentials.getGrantType());
        assertEquals(creds.getAuthType(), credentials.getAuthType());
        assertEquals(creds.getClientId(), credentials.getClientId());
        assertEquals(creds.getClientSecret(), credentials.getClientSecret());
        assertEquals(creds.getAuthHeaders(), credentials.getAuthHeaders());
    }

    @Test
    void saveSubscriptionWithMissingCredentialsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-creds.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));

        CredentialsInfo creds = new CredentialsInfo();
        creds.setAuthType("OAuth2");
        assertThrows(MissingArgumentException.class, () -> subscriptionService.save(request, creds), "Missing apiKey field");

        creds.setApiKey("api-key-test");
        creds.setAuthType(null);
        assertThrows(MissingArgumentException.class, () -> subscriptionService.save(request, creds), "Missing authType field");
    }

    @Test
    void deleteSubscriptionWithCredentialsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test-creds-delete.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));

        CredentialsInfo creds = new CredentialsInfo();
        creds.setClientId("clientId");
        creds.setTokenUrl("tokenUrl");
        creds.setClientSecret("clientSecret");
        creds.setGrantType("grantType");
        creds.setAuthType("OAuth2");
        creds.setApiKey("api-key-test");
        creds.setAuthHeaders("authHeaders");
        NsSubscriptionResponse response = subscriptionService.save(request, creds);
        assertNotNull(response);
        UUID id = response.getId();
        assertEquals(response, subscriptionService.get(id));

        Credentials credentials = repository.findById(id).get().getCredentials();
        assertNotNull(credentials);
        subscriptionService.delete(id);
        assertThrows(NoSuchElementException.class, () -> subscriptionService.get(id));
    }

    @Test
    void getInvalidIdTest() {
        UUID id = UUID.fromString("8c878e6f-ee13-4a37-a208-7510c2638900");
        assertThrows(NoSuchElementException.class, () -> subscriptionService.get(id));
    }

    @Test
    void deleteInvalidIdTest() {
        UUID id = UUID.fromString("8c878e6f-ee13-4a37-a208-7510c2638900");
        assertThrows(NoSuchElementException.class, () -> subscriptionService.get(id));
    }

    @Test
    void saveInvalidAddressTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("just_string");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.save(request), "field 'address' must to be URL");
    }

    @Test
    void saveInvalidFilterCriteriaTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("just_string");
        request.setSubscriptionFilter(List.of(filter));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.save(request),
                     "field 'filterCriteria' has invalid RSQL format. filterCriteria=" + filter.getFilterCriteria());
    }

    @Test
    void saveInvalidFieldsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setTenant("master");
        request.setAddress("http://test.com");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, \"invalid json key");
        request.setSubscriptionFilter(List.of(filter));
        assertThrows(IllegalArgumentException.class, () -> subscriptionService.save(request),
                     "field 'fields' contains invalid json keys. fields=" + filter.getFields());
    }

    @Test
    void saveMissingMandatoryFieldsTest() {
        NsSubscriptionRequest request = new NsSubscriptionRequest();
        request.setAddress("http://test.com");
        request.setTenant("master");
        assertThrows(MissingArgumentException.class, () -> subscriptionService.save(request),
                     "Mandatory field 'subscriptionFilter' missing or empty");

        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCompleted");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.type, event.priority");
        request.setSubscriptionFilter(List.of(filter));
        request.setTenant(null);
        assertThrows(MissingArgumentException.class, () -> subscriptionService.save(request),
                     "Field 'tenant' must not be null. If no tenant expected, set to empty string");

        request.setTenant("");
        NsSubscriptionResponse response = subscriptionService.save(request);
        assertNotNull(response);
        assertEquals("", response.getTenant());
    }
}