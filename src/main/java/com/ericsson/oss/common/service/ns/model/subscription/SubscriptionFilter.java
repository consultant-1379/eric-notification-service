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

package com.ericsson.oss.common.service.ns.model.subscription;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table (name = "subscription_event_type")
public class SubscriptionFilter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private int id;

    @Column (nullable = false)
    private String eventType;

    @Column (nullable = true)
    private String filterCriteria;

    @Column (nullable = true)
    private String fields;

    @Column (nullable = false)
    @JsonIgnore
    private int hash;

    public SubscriptionFilter() {
    }

    public SubscriptionFilter(final int id, final String eventType, final String filterCriteria, final String fields, final int hash) {
        this.id = id;
        this.eventType = eventType;
        this.filterCriteria = filterCriteria;
        this.fields = fields;
        this.hash = hash;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(final String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(final String fields) {
        this.fields = fields;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(final int hash) {
        this.hash = hash;
    }
}