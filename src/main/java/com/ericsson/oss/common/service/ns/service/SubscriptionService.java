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
package com.ericsson.oss.common.service.ns.service;

import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.exception.DuplicateSubscriptionException;
import com.ericsson.oss.common.service.ns.exception.MissingArgumentException;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Interface of the service implementing the business logic behind the Subscription REST interface.
 */
public interface SubscriptionService {

  /**
   * Create subscription. Validate a subscriptionCreation request and, if valid, stores it into the ENS database. Method is
   * synchronized to avoid race condition between different simultaneous creation requests. If some validation fails, the
   * validateXXX methods throw the proper exception.
   *
   * @param subscription The subscription to be created
   * @return The created subscription, including the assigned subscription id
   * @throws MissingArgumentException if one of the mandatory subscription params (address, tenant (can be empty string but not null), eventType) is
   * missing
   * @throws IllegalArgumentException if one of a subscription params is invalid
   * @throws DuplicateSubscriptionException if the same subscription already exists
   */
  NsSubscriptionResponse save(NsSubscriptionRequest subscription);

    /**
     * Create subscription. Validate a subscriptionCreation request and, if valid, stores it into the ENS database along with credential data.
     * Method is synchronized to avoid race condition between different simultaneous creation requests. If some validation fails, the
     * validateXXX methods throw the proper exception.
     *
     * @param subscription The subscription to be created
     * @param credentials The credentials provided
     * @return The created subscription, including the assigned subscription id
     * @throws MissingArgumentException if one of the mandatory subscription params (address, tenant (can be empty string but not null), eventType) is missing
     * @throws IllegalArgumentException if one of a subscription params is invalid
     * @throws DuplicateSubscriptionException if the same subscription already exists
     */
    NsSubscriptionResponse save(NsSubscriptionRequest subscription, CredentialsInfo credentials);

  /**
   * Delete a subscription with a given identifier. 
   * @param id The id of the subscription to delete
   * @throws NoSuchElementException if a subscription with provided id doesn't exist in db
   */
  void delete(UUID id);

  /**
   * Get the subscription with the given id.
   * @param id The id of the subscription to be retrieved
   * @return The subscription, if found.
   * @throws NoSuchElementException if a subscription with provided id doesn't exist in db
   */
  NsSubscriptionResponse get(UUID id);

  /**
   * Get all the subscriptions in NS database.
   * @return The list (possibly empty) of matching subscriptions
   */
  List<NsSubscriptionResponse> getAll();
}
