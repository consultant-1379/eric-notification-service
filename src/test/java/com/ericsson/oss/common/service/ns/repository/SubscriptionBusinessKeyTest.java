/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SubscriptionBusinessKeyTest {

    @Test
    void shouldReturnHash() {
        SubscriptionBusinessKey key = SubscriptionBusinessKey.builder()
                .address("http://client.us.com")
                .tenant("master")
                .eventType("OrderDeleted")
                .filterCriteria("event.priority==4")
                .build();
        assertEquals(-1120972243, key.getHash());
    }

    @Test
    void shouldReturnHashOnlyRequiredFields() {
        SubscriptionBusinessKey key = SubscriptionBusinessKey.builder()
                .address("http://client.uk.com")
                .eventType("OrderCanceled")
                .build();
        assertEquals(627050669, key.getHash());
    }
}