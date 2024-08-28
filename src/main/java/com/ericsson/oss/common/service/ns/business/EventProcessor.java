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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.ericsson.oss.common.service.ns.infrastructure.configurations.WebClientConfig;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.service.CredentialServiceImpl;
import com.ericsson.oss.common.service.ns.service.CredentialsMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.ericsson.oss.common.service.ns.api.model.NsEvent;
import com.ericsson.oss.common.service.ns.filter.EventPayload;
import com.ericsson.oss.common.service.ns.filter.RsqlFilter;
import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.model.subscription.SubscriptionFilter;
import com.ericsson.oss.common.service.ns.performance.PerformanceMetrics;
import com.ericsson.oss.common.service.ns.repository.DispatchRepository;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;
import com.ericsson.oss.common.service.ns.util.Utils;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;

import static com.ericsson.oss.common.service.ns.service.CredentialsMapper.convertToDto;

/**
 * Implements the processing of the event coming from the message bus.
 */
@Service
public class EventProcessor {

  private final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

  /**
   * The database table of subscriptions.
   */
  @Autowired
  private SubscriptionRepository subscriptionTable;

  /**
   * The database dispatch table.
   */
  @Autowired
  private DispatchRepository dispatchTable;

  /**
   * CredentialService used to get Content-Type
   */
    @Autowired
    private CredentialServiceImpl credentialService;

  /**
   * WebClientConfig used to get the webclient instance with ssl context
   */
  @Autowired
  private WebClientConfig webClientConfig;

  /**
   * The service responsible for metrics collection.
   */
  @Autowired
  private PerformanceMetrics metrics;

  /**
   * The minimum number of alive threads.
   */
  @Value("${dispatcher.min-threads}")
  private int minThreads;

  /**
   * The maximum number of alive threads.
   */
  @Value("${dispatcher.max-threads}")
  private int maxThreads;

  /**
   * The timeout (seconds) to be waited until an inactive thread is released.
   */
  @Value("${dispatcher.thread-timeout}")
  private int threadTimeout;

  /**
   * The time (milliseconds) to be waited between each retry sending a notification.
   */
  @Value("${restClient.retry.delay}")
  private long retryDelay;

  /**
   * The number of retries.
   */
  @Value("${restClient.retry.times}")
  private long retryTimes;

  /**
   * The thread pool serving the delivery process.
   */
  private ExecutorService executor;

  /**
   * The event being managed.
   */
  private NsEvent event;

  /**
   * Post initialization allowing to create the configurable thread executor after the injection of its parameters from the property
   * file.
   */
  @PostConstruct
  public void init() {
    executor = new ThreadPoolExecutor(minThreads, maxThreads, threadTimeout, TimeUnit.SECONDS, new SynchronousQueue<>());
  }

  /**
   * The procedure in charge of processing a message bus event. Do the following operations:
   * 1. Check for the event correctness
   * 2. Check if there are still pending notifications for that eventId. If any manage them
   * 3. If no pending notification performs filtering and projection transformation on the event payload and deliver the
   * relevant notifications to destination
   * @param msgBusEvent The event coming from the Message Bus
   */
  public void process(String msgBusEvent) {
    Sample s = Timer.start();
    metrics.getEventCounter().increment();
    if (checkEvent(msgBusEvent)) {
      logger.info("Received event -> Event ID : {} , Payload : {}, EventTime : {} , Tenant: {}, Descriptor: {} " , event.getEventID(), event.getPayLoad(), event.getEventTime(),event.getTenant(), event.getDescriptor());
      List<Dispatch> pendingNotifications = dispatchTable.findAll(event.getEventID());
      if (pendingNotifications.isEmpty()) {
        manageEvent();
      } else {
        managePendingNotifications(pendingNotifications);
      }
      s.stop(metrics.getEventManagementTime());
    } else {
      logger.error("Invalid notification syntax");
    }
  }

  /**
   * Procedure to manage a new message bus event.
   */
  private void manageEvent() {
    var payloadInfo = new EventPayload(event.getPayLoad());

    var oauthHeader = MediaType.APPLICATION_JSON_VALUE;
    // Compute the list of subscriptions matching event type, tenant and filter
    List<Subscription> subscriptions = getMatchingSubscriptions(payloadInfo);
    if (!subscriptions.isEmpty()) {
      Map<String, Dispatch> dispatchRecords = new HashMap<>();
      calculateNotifications(payloadInfo, oauthHeader, subscriptions, dispatchRecords);
      // Put the information about the current notification into the current_dispatch table of the database.
      dispatchTable.save(new ArrayList<>(dispatchRecords.values()));
      var latch = new CountDownLatch(dispatchRecords.size());
      var deliveryContext = createDeliveryContext();
      for (Dispatch d: dispatchRecords.values()) {
        executor.execute(new NotificationDispatcher(d.getEventId(), d.getAddress(), d.getHash(),
                (d.getHash() != 0 ? d.getPayload() : event.getPayLoad()), d.getOauthRegistrationId(), deliveryContext, metrics, latch,
                event.getAdditionalInformation(), d.getAuthHeaders(), d.getCredentialsInfo()));
      }
      waitForDispatchCompletion(latch);
    }
  }

private void calculateNotifications(EventPayload payloadInfo, String oauthHeader,
      List<Subscription> subscriptions, Map<String, Dispatch> dispatchRecords) {
    for (Subscription subscription : subscriptions) {
      String oauthRegistrationId;
      CredentialsInfo credentialsInfo;
        String payload = event.getPayLoad();
        // If the fields are defined, computes the projection and produces a reduced payload
        SubscriptionFilter filter = subscription.getSubscriptionFilter().get(0);
        Integer hash = 0;
        if (!StringUtils.isEmpty(filter.getFields())) {
          metrics.getProjNotifCounter().increment();
          List<String> fields = Arrays.stream(filter.getFields().split(",")).map(String::trim).collect(Collectors.toList());
          List<String> actualFields = new ArrayList<>();
          fields.forEach(field -> actualFields.add(Utils.convertPath(field)));
          payload = payloadInfo.doProjection(actualFields);
          hash = payload.hashCode();
        }
        if (subscription.getCredentials() != null) {
          oauthRegistrationId = subscription.getCredentials().getId().toString();
          credentialsInfo = convertToDto(subscription.getCredentials());
          Optional<String> header = credentialService.getContentType(subscription.getCredentials().getAuthHeaders());
          if (header.isPresent()) {
            oauthHeader = header.get();
          }
        } else {
          credentialsInfo = null;
          oauthRegistrationId = null;
          oauthHeader = MediaType.APPLICATION_JSON_VALUE;
        }
        var dispatchRecord = new Dispatch(event.getEventID(), subscription.getAddress(), hash, (hash != 0 ? payload : null), oauthRegistrationId, oauthHeader, credentialsInfo);
        dispatchRecords.putIfAbsent(dispatchRecord.getKey(), dispatchRecord);
    }
}

  /**
   * Wait to be sure all the notification threads are up and running. If this is not the case, it means that some threads have been
   * queued (because we have too many of them around), so it's better to slow down message bus reading activity.
   * @param latch The latch to be waited for (each NotificationDispatcher decrements the latch as soon as it's started)
   */
  private void waitForDispatchCompletion(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException ex) {
      logger.error("Dispatching interrupted for event {}", event.getEventID());
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Procedure to manage pending notifications.
   * Initialize the latch to the number of pending notifications to be dispatched and for each of them start a
   * NotificationDispatcher thread in charge to deliver the notification.
   * @param pendingNotifications  The list of notifications still to be delivered
   */
  private void managePendingNotifications(List<Dispatch> pendingNotifications) {
    var latch = new CountDownLatch(pendingNotifications.size());
    var deliveryContext = createDeliveryContext();
    pendingNotifications.forEach(pendingNotification -> executor.execute(new NotificationDispatcher(event.getEventID(),
            pendingNotification.getAddress(), pendingNotification.getHash(),
            StringUtils.isEmpty(pendingNotification.getPayload()) ? event.getPayLoad() : pendingNotification.getPayload(),
            pendingNotification.getOauthRegistrationId(), deliveryContext, metrics, latch, event.getAdditionalInformation(), pendingNotification.getAuthHeaders(), pendingNotification.getCredentialsInfo())));
    waitForDispatchCompletion(latch);
  }

  /**
   * Check for the correctness of the event. Check that all the mandatory fields are present, and that the payload field is a valid
   * JSON string
   *
   * @param kafkaEvent The event read from the Kafka bus
   * @return True if the event is correct, false otherwise
   */
  private boolean checkEvent(String kafkaEvent) {
    event = Utils.deserialize(kafkaEvent, NsEvent.class);
    if (event == null) {
      logger.error("Invalid notification received on topic <event>");
      return false;
    }
    if (StringUtils.isEmpty(event.getEventID())) {
      return false;
    }
    if (event.getEventTime() == null) {
      return false;
    }
    if (event.getEventType() == null) {
      return false;
    }
    if (event.getTenant() == null) {
      return false;
    }
    if (event.getPayLoad() == null) {
      return false;
    }
    return Utils.isJsonDataValid(event.getPayLoad());
  }

  /**
   * Retrieve the subscriptions matching the event.
   *
   * @param payload of the event
   * @return The list of matching subscriptions
   */
  private List<Subscription> getMatchingSubscriptions(EventPayload payload) {
    List<Subscription> matchingSubscriptions = subscriptionTable.findByEventTypeAndTenant(event.getEventType(), event.getTenant());
    List<Subscription> matching = new ArrayList<>();
    if (matchingSubscriptions.isEmpty()) {
      logger.warn("No subscription matches received eventType/tenant");
      return matching;
    }
    matchingSubscriptions.forEach((Subscription s) -> {
      var subscriptionFilter = s.getSubscriptionFilter().get(0);
      if (!StringUtils.isEmpty(subscriptionFilter.getFilterCriteria())) {
        var filter = new RsqlFilter(subscriptionFilter.getFilterCriteria());
        if (Boolean.TRUE.equals(filter.eval(payload))) {
          matching.add(s);
        } else {
          metrics.getDroppedNotifCounter().increment();
        }
      } else {
        matching.add(s);
      }
    });
    return matching;
  }

  /**
   *  Create the delivery context and updates webClient with latest truststore
   */
  private DeliveryContext createDeliveryContext() {
    var webClient = webClientConfig.createWebClient();
    return new DeliveryContext(retryDelay, retryTimes, dispatchTable, webClient);
  }
}
