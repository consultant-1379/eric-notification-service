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
package com.ericsson.oss.common.service.ns.msgbus.kafka;

import com.ericsson.oss.common.service.ns.business.EventProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Consumer of Kafka events. For each received event invokes the EventProcessor to create and dispatch the relevant notifications.
 */
@ConditionalOnProperty("spring.kafka.enabled")
@Service
public class KafkaEventConsumer {

  @Autowired
  private EventProcessor processor;

  /**
   * The Kafka listener. Receive the Kafka event as a String, then de-serialize it into a NotificationEvent structure, get the
   * relevant notifications, for each notification match the event against its filter, and for each matching notification start a
   * set of dispatcher threads. When all the threads have finished, acknowledge kafka.
   *
   * @param kafkaEvent The kafka event to be dispatched
   * @param ack The kafka structure to provide manual acknowledgment
   */
  @KafkaListener(topics = "${spring.kafka.event-topic}", groupId = "${spring.kafka.event-group}")
  public synchronized void receive(String kafkaEvent, Acknowledgment ack) {
    processor.process(kafkaEvent);
    ack.acknowledge();
  }
}
