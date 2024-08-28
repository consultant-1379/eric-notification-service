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
package com.ericsson.oss.common.service.ns.repository;

import lombok.Builder;

/**
 * Present structure that defines a uniqueness of a subscription.
 * Created for the sake having hashCode calculation logic in one place.
 * Hashcode is used during saving a subscription in db and searching if duplicates for a subscription already exists
 */
@Builder
public class SubscriptionBusinessKey {

    private final String address;

    private final String tenant;

    private final String eventType;

    private final String filterCriteria;

    public int getHash() {
        return (address + tenant + eventType + filterCriteria).hashCode();
    }
}