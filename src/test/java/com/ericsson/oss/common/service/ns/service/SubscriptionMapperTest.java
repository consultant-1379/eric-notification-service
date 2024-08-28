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

package com.ericsson.oss.common.service.ns.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionFilter;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.model.subscription.SubscriptionFilter;

class SubscriptionMapperTest {

    @Test
    void shouldMapDtoToEntity() {
        NsSubscriptionRequest dto = new NsSubscriptionRequest();
        dto.setAddress("http://test.com");
        dto.setTenant("master");
        NsSubscriptionFilter filter = new NsSubscriptionFilter();
        filter.setEventType("OrderCreated");
        filter.setFilterCriteria("event.priority==1");
        filter.setFields("event.priority,event.eventType");
        dto.setSubscriptionFilter(List.of(filter));

        Subscription entity = SubscriptionMapper.convertToEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getAddress(), entity.getAddress());
        assertEquals(dto.getTenant(), entity.getTenant());
        assertNotNull(entity.getSubscriptionFilter());
        assertEquals(entity.getSubscriptionFilter().size(), dto.getSubscriptionFilter().size());
        assertEquals(entity.getSubscriptionFilter().get(0).getEventType(), dto.getSubscriptionFilter().get(0).getEventType());
        assertEquals(entity.getSubscriptionFilter().get(0).getFilterCriteria(), dto.getSubscriptionFilter().get(0).getFilterCriteria());
        assertEquals(entity.getSubscriptionFilter().get(0).getFields(), dto.getSubscriptionFilter().get(0).getFields());
    }

    @Test
    void shouldMapEntityToDto() {
        SubscriptionFilter filter = new SubscriptionFilter();
        filter.setEventType("OrderCreated");
        filter.setFilterCriteria("event.priority=1");
        filter.setFields("event.priority,event.eventType");
        Subscription entity = new Subscription(UUID.randomUUID(), "master", "http://test.com", List.of(filter));

        NsSubscriptionResponse dto = SubscriptionMapper.convertToDto(entity);

        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getAddress(), dto.getAddress());
        assertEquals(entity.getTenant(), dto.getTenant());
        assertNotNull(dto.getSubscriptionFilter());
        assertEquals(dto.getSubscriptionFilter().size(), entity.getSubscriptionFilter().size());
        assertEquals(dto.getSubscriptionFilter().get(0).getEventType(), entity.getSubscriptionFilter().get(0).getEventType());
        assertEquals(dto.getSubscriptionFilter().get(0).getFilterCriteria(), entity.getSubscriptionFilter().get(0).getFilterCriteria());
        assertEquals(dto.getSubscriptionFilter().get(0).getFields(), entity.getSubscriptionFilter().get(0).getFields());
    }
}