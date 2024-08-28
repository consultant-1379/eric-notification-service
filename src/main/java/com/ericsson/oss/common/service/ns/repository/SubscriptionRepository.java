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

package com.ericsson.oss.common.service.ns.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ericsson.oss.common.service.ns.model.subscription.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    @Query(nativeQuery = true, value = "SELECT * FROM subscription s LEFT JOIN subscription_event_type t ON s.id=t.subscription_id WHERE t.hash=?1")
    List<Subscription> findByHash(int hash);

    @Query (nativeQuery = true, value = "select s.id, s.address, s.tenant, s.credentials_id, t.event_type, t.filter_criteria, t.fields FROM "
            + "subscription s LEFT JOIN subscription_event_type t ON s.id=t.subscription_id WHERE (t.event_type=?1 and s.tenant=?2)")
    List<Subscription> findByEventTypeAndTenant(String eventType, String tenant);
}