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

import com.ericsson.oss.common.service.ns.repository.DispatchRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Keeps delivery parameters for notification dispatching.
 */
@Getter
@AllArgsConstructor
public class DeliveryContext {
  /**
   * The time to be waited between each retry sending a notification.
   */
  long retryDelay;
  /**
   * The number of attempts done to send successfully a notification.
   */
  long retryTimes;
  /**
   * The current_dispatch repository. Used to avoid loosing or duplicating notifications.
   */
  DispatchRepository dispatchTable;
  /***
   * WebClient instance used to send rest calls
   */
  WebClient webClient;
}
