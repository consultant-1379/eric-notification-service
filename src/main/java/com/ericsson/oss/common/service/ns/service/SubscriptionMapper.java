/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionFilter;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.model.subscription.SubscriptionFilter;
import com.ericsson.oss.common.service.ns.repository.SubscriptionBusinessKey;

/**
 * Converter class to map Subscription records into NsSubscriptionResponse and NsSubscriptionRequest into Subscription.
 */
public class SubscriptionMapper {

    // Utility classes should not have public constructors
    private SubscriptionMapper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convert Subscription into NsSubscriptionResponse
     * @param entity The Subscription entity to be converted
     * @return The returned NsSubscriptionResponse structure
     */
    public static NsSubscriptionResponse convertToDto(Subscription entity) {
        if (entity == null) {
            return null;
        }
        var dto = new NsSubscriptionResponse();
        dto.setId(entity.getId());
        dto.setAddress(entity.getAddress());
        dto.setTenant(entity.getTenant());
        List<NsSubscriptionFilter> filters = new ArrayList<>();
        entity.getSubscriptionFilter().forEach(f -> {
            var filter = new NsSubscriptionFilter();
            filter.setEventType(f.getEventType());
            filter.setFilterCriteria(f.getFilterCriteria());
            filter.setFields(f.getFields());
            filters.add(filter);
        });
        dto.setSubscriptionFilter(filters);
        return dto;
    }

    /**
     * Convert an NsSubscriptionRequest into a Subscription record.
     * @param dto The NsSubscriptionRequest to be converted
     * @return The returned Subscription record
     */
    public static Subscription convertToEntity(NsSubscriptionRequest dto) {
        if (dto == null) {
            return null;
        }
        var entity = new Subscription();
        entity.setAddress(dto.getAddress());
        entity.setTenant(dto.getTenant());
        List<SubscriptionFilter> filters = new ArrayList<>();
        dto.getSubscriptionFilter().forEach(f -> {
            var filter = new SubscriptionFilter();
            filter.setEventType(f.getEventType());
            filter.setFilterCriteria(f.getFilterCriteria());
            filter.setFields(f.getFields());
            SubscriptionBusinessKey key = SubscriptionBusinessKey.builder()
                    .address(entity.getAddress())
                    .tenant(entity.getTenant())
                    .eventType(filter.getEventType())
                    .filterCriteria(filter.getFilterCriteria())
                    .build();
            filter.setHash(key.getHash());
            filters.add(filter);
        });
        entity.setSubscriptionFilter(filters);
        return entity;
    }
}
