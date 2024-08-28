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
package com.ericsson.oss.common.service.ns.controller.subscription;

import com.ericsson.oss.common.service.ns.api.SubscriptionApi;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.common.service.ns.service.CredentialService;
import com.ericsson.oss.common.service.ns.service.SubscriptionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class provides the implementation of the REST interface of the Notification Service, managing the subscription related
 * operations.
 */
@RestController
@RequiredArgsConstructor
public class SubscriptionApiControllerImpl implements SubscriptionApi {

  /**
   * The SubscriptionService bean (injected through the RequiredArgsConstructor) implementing the business logic to manage the
   * subscriptions.
   */
  private final SubscriptionService subscriptionService;
  /**
   * The PerformanceMetrics bean.
   */
  private final PerformanceMetrics metrics;
  /**
   * The CredentialService bean.
   */
  private final CredentialService credentialService;

  /**
   * The request context bean.
   */
  @Autowired
  private RequestContext requestContext;

  /**
   * Manages the creation of a new subscription.
   * @param request The subscription request
   * @param apiKey The apikey that can be used to fetch further connection details from external system.
   * @return A response entity with the SubscriptionResponse body (in case of successful creation)
   */
  @Override
  public ResponseEntity<NsSubscriptionResponse> createSubscription(@Valid NsSubscriptionRequest request, @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
    Optional<String> apiKeyHeader = Optional.ofNullable(apiKey);
    NsSubscriptionResponse nsSubscriptionResponse;
    requestContext.setOperation("create");
    CredentialsInfo credentialsInfo;
    if (apiKeyHeader.isPresent()) {
      credentialsInfo = credentialService.getCredentials(apiKeyHeader.get(), request.getAddress());
      nsSubscriptionResponse = subscriptionService.save(request, credentialsInfo);
    } else {
      nsSubscriptionResponse = subscriptionService.save(request);
    }
    metrics.getCreateSubscrCounter().increment();

    return new ResponseEntity<>(nsSubscriptionResponse, HttpStatus.CREATED);
  }

  /**
   * Manages the deletion of a subscription with a given identifier.
   * @param id The identifier of the subscription to be deleted
   * @return A response with empty body
   */
  @Override
  public ResponseEntity<Void> deleteSubscription(@PathVariable("id") UUID id) {
    requestContext.setOperation("delete");
    subscriptionService.delete(id);
    metrics.getDeleteSubscrCounter().increment();
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Manages the retrieval of a subscription with a given identifier.
   * @param id The identifier of the subscription to be returned
   * @return A Response entity with the SubscriptionResponse body for the retrieved subscription
   */
  @Override
  public ResponseEntity<NsSubscriptionResponse> getSubscription(@PathVariable("id") UUID id) {
    requestContext.setOperation("get");
    NsSubscriptionResponse resp = subscriptionService.get(id);
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  /**
   * Get all the subscriptions in the database. 
   * @return A Response entity carrying the list of all the subscriptions (one SubscriptionResponse per entry)
   */
  @Override
  public ResponseEntity<List<NsSubscriptionResponse>> getSubscriptions() {
    requestContext.setOperation("get");
    List<NsSubscriptionResponse> resp = subscriptionService.getAll();
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }
}
