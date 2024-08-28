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
package com.ericsson.oss.common.service.ns.business;

import com.ericsson.oss.common.service.ns.model.OAuthTokenRequest;
import com.ericsson.oss.common.service.ns.model.additional.properties.AdditionalInformation;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.common.service.ns.util.Utils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.ericsson.oss.common.service.ns.util.Constants.AUTH_JSON_TOKEN_REQ;
import static com.ericsson.oss.common.service.ns.util.Constants.OAUTH_ACCESS_TOKEN;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

/**
 * Runnable class implementing the process dispatch a notification.
 *
 * 1. Check for the correctness of the event to be dispatched.
 *
 * 2. Looks for the matching subscribers
 *
 * 3. Forwards the event as a POST to each subscriber destination
 *
 */
public class NotificationDispatcher implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);

  /**
   * The eventId of the event to be dispatched.
   */
  private final String eventId;
  /**
   * The address of the client.
   */
  private final String address;
  /**
   * The hash of the payload (or 0 is payload is missing/null).
   */
  private final Integer hash;
  /**
   * The payload to be delivered.
   */
  private final String payload;
  /***
   * The unique registrationId for oauth2 flow
   */
  private final String oauthRegistrationId;
  /**
   * The additionalInformation to dispatch
   */
  private AdditionalInformation additionalInformation;
  /**
   * A latch used to understand when all the notification dispatchers have completed.
   */
  private final CountDownLatch latch;
  /**
   * The context structure with the parameters to dispatch the notification.
   */
  private final DeliveryContext deliveryCtx;
  /**
   * The structure with all the metrics counters.
   */
  private final PerformanceMetrics metricsCtx;

  private final String authHeaders;

  private final CredentialsInfo credentialsInfo;

  /**
   * Constructor of the NotificationDispatcher. Includes the event and the sending parameters which cannot be auto-wired as this
   * class is not a bean.
   *
   * @param eventId The eventId of the event to be dispatched
   * @param address The address of the destination client (URL)
   * @param payload The payload of the notification
   * @param oauthRegistrationId The unique registrationId for oauth2 flow
   * @param deliveryCtx The notification delivery parameters (retryDelay, retryTimes and restTemplate)
   * @param metricsCtx The metrics context with all the metrics about notifications and events
   * @param latch The latch used by the EventProcessor to wait for all notifications delivered
   */
  public NotificationDispatcher(String eventId, String address, Integer hash, String payload, String oauthRegistrationId, DeliveryContext deliveryCtx,
                                PerformanceMetrics metricsCtx, CountDownLatch latch, String additionalInformation, String authHeaders, CredentialsInfo credentialsInfo) {
    this.eventId = eventId;
    this.address = address;
    this.payload = payload;
    this.oauthRegistrationId = oauthRegistrationId;
    this.hash = hash;
    this.deliveryCtx = deliveryCtx;
    this.metricsCtx = metricsCtx;
    this.latch = latch;
    this.additionalInformation = Utils.deserialize(additionalInformation, AdditionalInformation.class);
    this.authHeaders = authHeaders;
    this.credentialsInfo = credentialsInfo;
  }

  /**
   * The dispatching procedure.
   */
  @Override
  public void run() {
    logger.info("Trying to deliver notification for event {} to {}", eventId, address);
    metricsCtx.getPendingNotificationsCounter().getAndIncrement();
    // Try a retryTimes times to send the notification
    for (var i = 0; i < deliveryCtx.retryTimes; ++i) {
      var success = deliverNotification();
      if (i == 0) { // After first delivery attempt the delivery is considered done and the relevant record removed from the DB
        latch.countDown(); // Decrement the latch used by the event processor to wait for all threads running
        deliveryCtx.dispatchTable.delete(eventId, address, hash);
      }
      if (success) {
        metricsCtx.getPendingNotificationsCounter().getAndDecrement();
        if (i == 0) {
          metricsCtx.getSuccNotifCounter().increment();
          return;
        } else {
          metricsCtx.getSuccRetriedNotifCounter().increment();
          return;
        }
      }
      logger.info("Retry sending to {}", address);
      // Wait to retry
      Utils.sleep(deliveryCtx.retryDelay);
    }
    logger.error("Failure dispatching notification to {}", address);
    metricsCtx.getFailNotifCounter().increment();
    metricsCtx.getPendingNotificationsCounter().getAndDecrement();
  }

  /**
   * Try and send the notification to the client. In case of failure the relevant subscription is put inactive if the maximum
   * retransmission count is exceeded.
   *
   * @return True if the post was successful
   */
  private boolean deliverNotification() {
    try {
      HttpStatus ret;
      if (credentialsInfo != null && StringUtils.equalsIgnoreCase(credentialsInfo.getAuthTokenRequest(), AUTH_JSON_TOKEN_REQ)) {
        String response = sendPostOauthToken();
        String token = getTokenValue(response);
        ret = sendPostOAuthRequest(token);
      } else {
        ret = sendPost(address, payload);
      }
      if(ret != null) {
      if (ret.is5xxServerError()) {
        logger.error("Notification failed sent to {} with HttpStatus {}", address, ret.value());
        return false;
      }
      logger.info("Notification is dispatched to {} with HttpStatus {}", address, ret.value());
      }
    } catch (Exception ex) {
      logger.error("Notification failed sent to {} with error {}", address, ex.getMessage());
      logger.error("Full exception is : {}", ex);
      return false;
    }
    return true;
  }

  /**
   * Send a POST with the given body to a specified url
   *
   * @param url The url to send the POST
   * @param body The body of the message
   */
  private HttpStatus sendPost(String url, String body) {
    try {
      ResponseEntity<Void> responseEntity =  this.deliveryCtx.webClient.post().uri(url)
              .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.valueOf(authHeaders));
                addCustomHeaders(httpHeaders);
              })
              .bodyValue(body)
              .attributes(updateClientRegistrationId())
              .retrieve()
              .toBodilessEntity()
              .block();
      return responseEntity.getStatusCode();
    } catch (WebClientResponseException e) {
      return e.getStatusCode();
    }
  }

  private Consumer<Map<String, Object>> updateClientRegistrationId() {
    if (StringUtils.isNotEmpty(oauthRegistrationId))
      return clientRegistrationId(oauthRegistrationId);
    return consumer -> {};
  }

  private void addCustomHeaders(HttpHeaders httpHeaders) {
    if(additionalInformation != null){
      Map<String, String> customHeaders = additionalInformation.getHeaders();
      if (customHeaders != null)
        customHeaders.entrySet().forEach(customHeader -> httpHeaders.set(customHeader.getKey(), customHeader.getValue()));
    }
  }

  private String sendPostOauthToken() {
    try {
      logger.debug("Token request : {} to url : {} ", getOAuthTokenRequest(), credentialsInfo.getTokenUrl());
      ResponseEntity<String> response = this.deliveryCtx.webClient.post().uri(credentialsInfo.getTokenUrl())
              .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                addCustomHeaders(httpHeaders);
              }).bodyValue(getOAuthTokenRequest())
              .retrieve()
              .toEntity(String.class)
              .block();
      logger.debug("Token response : {}", response.getBody());
      return response.getBody();
    }  catch (WebClientResponseException e) {
      logger.error("Error occurred while retrieving the token : {} ", e);
    }
    return null;
  }

  private String getTokenValue(String response) {
    ObjectNode node = Utils.deserialize(response, ObjectNode.class);
    if (node.has(OAUTH_ACCESS_TOKEN)) {
      return node.get(OAUTH_ACCESS_TOKEN).textValue();
    }
    return null;
  }

  private HttpStatus sendPostOAuthRequest(String token) {
    try {
      logger.debug("OAuth request : {} to url : {}", payload, address);
      ResponseEntity<Void> responseEntity =  this.deliveryCtx.webClient.post().uri(address)
              .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.valueOf(authHeaders));
                httpHeaders.setBearerAuth(token);
                addCustomHeaders(httpHeaders);
              })
              .bodyValue(payload)
              .retrieve()
              .toBodilessEntity()
              .block();
      return responseEntity.getStatusCode();
    } catch (WebClientResponseException e) {
      logger.error("Exception occurred while sending OAuth request : {}", e);
      return e.getStatusCode();
    }
  }

  private String getOAuthTokenRequest() {
    OAuthTokenRequest oAuthTokenRequest = new OAuthTokenRequest();
    oAuthTokenRequest.setClientId(credentialsInfo.getClientId());
    oAuthTokenRequest.setClientSecret(credentialsInfo.getClientSecret());
    oAuthTokenRequest.setGrantType(credentialsInfo.getGrantType());
    return Utils.serialize(oAuthTokenRequest);
  }

}
