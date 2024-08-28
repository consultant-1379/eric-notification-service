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

/**
 * Includes the business logic of the notification service. The Event processor processes the event coming from one of the
 * enabled message buses, executing the following operations: management of pending events, correlation with active subscriptions, 
 * filtering, projection and generation of the relevant notifications. 
 * The Event dispatcher (one instance per delivery) distributes each notification to the relevant client (managing issues with the
 * connection through retries, as needed).
 */
package com.ericsson.oss.common.service.ns.business;
