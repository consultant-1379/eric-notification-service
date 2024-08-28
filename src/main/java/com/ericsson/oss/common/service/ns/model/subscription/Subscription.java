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

import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table (name = "subscription")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    @Column(nullable = false, name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column (nullable = false)
    private String tenant;

    @Column (nullable = false)
    private String address;

    //Adding fetch type to avoid lazy loading
    @OneToMany (cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn (name = "subscription_id", nullable = false)
    @JsonIgnore
    private List<SubscriptionFilter> subscriptionFilter;

    @OneToOne (cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn (name = "credentials_id")
    private Credentials credentials;


    public Subscription() {
    }

    public Subscription(final UUID id,
                        final String tenant,
                        final String address,
                        final List<SubscriptionFilter> subscriptionFilter) {
        this.id = id;
        this.tenant = tenant;
        this.address = address;
        this.subscriptionFilter = subscriptionFilter;
    }

    public Subscription(final UUID id,
                        final String tenant,
                        final String address,
                        final List<SubscriptionFilter> subscriptionFilter,
                        final Credentials credentials) {
        this.id = id;
        this.tenant = tenant;
        this.address = address;
        this.subscriptionFilter = subscriptionFilter;
        this.credentials = credentials;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public List<SubscriptionFilter> getSubscriptionFilter() {
        return subscriptionFilter;
    }

    public void setSubscriptionFilter(final List<SubscriptionFilter> subscriptionFilter) {
        this.subscriptionFilter = subscriptionFilter;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }
}