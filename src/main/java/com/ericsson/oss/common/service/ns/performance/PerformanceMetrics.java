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
package com.ericsson.oss.common.service.ns.performance;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;

/**
 * The PerformanceMetrics service manages the performance metrics to be collected by the Notification Service.
 */
@Getter
@Service
public class PerformanceMetrics {

  /**
   * The database table of subscriptions.
   */
  @Autowired
  private SubscriptionRepository subscriptionTable;
  /**
   * Counts the number of subscription creations.
   */
  private Counter createSubscrCounter;
  /**
   * Counts the number of subscription deletions.
   */
  private Counter deleteSubscrCounter;
  /**
   * Counts the number of failed subscription creations.
   */
  private Counter failCreateSubscrCounter;
  /**
   * Counts the number of failed subscription creations.
   */
  private Counter failDeleteSubscrCounter;
  /**
   * 
   */
  private Counter eventCounter; // Counts events received by the event processor
  /**
   * Counts the notifications successfully sent (without retry).
   */
  private Counter succNotifCounter;
  /**
   * Counts the notifications successfully sent (after retry).
   */
  private Counter succRetriedNotifCounter;
  /**
   * Counts the notifications failed after the configured retry count.
   */
  private Counter failNotifCounter;
  /**
   * Counts the notifications dropped as filter didn't pass.
   */
  private Counter droppedNotifCounter;
  /**
   * Counts the notifications whose payload has been "projected".
   */
  private Counter projNotifCounter;
  /**
   * Number of current active subscriptions.
   */
  private Gauge activeSubscriptions;
  /**
   * The time needed for the management of an event.
   */
  private Timer eventManagementTime;
  /**
   * Number of notifications waiting to be dispatched.
   */
  private Gauge awaitDispNotifications;
  /**
   * Number of notifications waiting to be dispatched.
   */
  private AtomicInteger pendingNotificationsCounter;
  /**
   * The micrometer registry instance injected by spring boot.
   */
  private final MeterRegistry meterRegistry;

  public PerformanceMetrics(MeterRegistry meterRegistry) {
    pendingNotificationsCounter = new AtomicInteger(0);
    this.meterRegistry = meterRegistry;
    createCounters();
  }

  /**
   * Clean all the counters (deleting and recreating all of them). Used in tests.
   */
  public void resetCounters() {
    meterRegistry.getMeters().forEach(meterRegistry::remove);
    createCounters();
  }

  /**
   * Create the metrics.
   */
  private void createCounters() {
    createSubscrCounter = Counter.builder("createsubscriptions.success")
            .description("The number of subscription creations")
            .register(meterRegistry);
    deleteSubscrCounter = Counter.builder("deletesubscriptions.success")
            .description("The number of subscription deletions")
            .register(meterRegistry);    
    failCreateSubscrCounter = Counter.builder("createsubscriptions.fail")
            .description("The number of failed subscription creations")
            .register(meterRegistry);
    failDeleteSubscrCounter = Counter.builder("deletesubscriptions.fail")
            .description("The number of failed subscribtion deletions")
            .register(meterRegistry);
    eventCounter = Counter.builder("events")
            .description("The number of events consumed by the notification service")
            .register(meterRegistry);
    succNotifCounter = Counter.builder("notifications.success.noretry")
            .description("The number of successfully delivered notifications without retry")
            .register(meterRegistry);
    succRetriedNotifCounter = Counter.builder("notifications.success.retried")
            .description("The number of successfully delivered notifications after retry")
            .register(meterRegistry);
    failNotifCounter = Counter.builder("notifications.fail")
            .description("The number of notifications failing to deliver")
            .register(meterRegistry);
    droppedNotifCounter = Counter.builder("notifications.dropped")
            .description("The number of notifications not passing the filter")
            .register(meterRegistry);
    projNotifCounter = Counter.builder("notifications.projected")
            .description("The number of notifications with projected payload")
            .register(meterRegistry);
    awaitDispNotifications = Gauge.builder("notifications.awaitingdispatch",this, value -> getPendingNotificationsCounter().doubleValue())
            .description("The number of notifications that are waiting to be dispatched")
            .register(meterRegistry);
    activeSubscriptions = Gauge.builder("activesubscriptions", this, PerformanceMetrics::getSubscriptionsCount)
            .description("The number of currently active subscriptions")
            .register(meterRegistry);
    eventManagementTime = Timer.builder("eventmantime")
            .description("The duration of the event management process")
            .register(meterRegistry);
  }

  /**
   * Local function to read from the database the number of subscription records. Used by the gauge metric storing the active
   * subscriptions number.
   * Uses provisional SubscriptionDb.
   * @return The number of active subscriptions
   */
  private double getSubscriptionsCount() {
    return subscriptionTable.count();
  }

  /**
   * Return the number of successful create Subscription operations.
   * @return The number of successful create Subscription operations
   */
  public int getCreateSubscriptionCount() {
    return (int) createSubscrCounter.count();
  }

  /**
   * Return the number of successful delete Subscription operations.
   * @return The number of successful delete Subscription operations
   */
  public int getDeleteSubscriptionCount() {
    return (int) deleteSubscrCounter.count();
  }

  /**
   * Return the number of failed create Subscription operations.
   * @return The number of failed create Subscription operations
   */
  public int getFailedCreateSubscriptionCount() {
    return (int) failCreateSubscrCounter.count();
  }

  /**
   * Return the number of failed delete Subscription operations.
   * @return The number of failed delete Subscription operations
   */
  public int getFailedDeleteSubscriptionCount() {
    return (int) failDeleteSubscrCounter.count();
  }

  /**
   * Return the mean time for managing an event (in milliseconds).
   * @return The mean event management time
   */
  public double getEventManagementTimeValue() {
    return eventManagementTime.mean(TimeUnit.MILLISECONDS);
  }

  /**
   * Return the number of active subscriptions.
   * @return Return as an integer the value of the metric gauge including the active subscriptions
   */
  public int getActiveSubscriptionsCount() {
    return (int)activeSubscriptions.value();
  }

  /**
   * Return the number of the events managed by the event processor.
   * @return The number of events
   */
  public int getEventCount() {
    return (int) eventCounter.count();
  }

  /**
   * Return the number of notifications successfully delivered (without retry).
   * @return The number of successfully delivered notifications
   */
  public int getSuccNotifCount() {
    return (int) succNotifCounter.count();
  }

  /**
   * Return the number of notifications successfully delivered (after retry).
   * @return The number of successfully delivered notifications after retry
   */
  public int getSuccRetriedNotifCount() {
    return (int) succRetriedNotifCounter.count();
  }

  /**
   * Return the number of notifications with failed delivery.
   * @return The number of failed notifications
   */
  public int getFailNotifCount() {
    return (int) failNotifCounter.count();
  }

  /**
   * Return the number of notifications not passing the filter.
   * @return The number of dropped notifications
   */
  public int getDroppedNotifCount() {
    return (int) droppedNotifCounter.count();
  }

  /**
   * Return the number of notification with modified payload due to projection.
   * @return The number of projected notifications
   */
  public int getProjNotifCount() {
    return (int) projNotifCounter.count();
  }

}
