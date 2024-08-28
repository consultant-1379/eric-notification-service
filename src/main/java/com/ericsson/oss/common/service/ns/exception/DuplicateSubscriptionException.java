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

package com.ericsson.oss.common.service.ns.exception;

import com.ericsson.oss.common.service.ns.model.subscription.Subscription;

public class DuplicateSubscriptionException extends RuntimeException {

    private final transient Subscription duplicate;

    public DuplicateSubscriptionException(Subscription duplicate) {
        super("A subscription with the same set of parameters already exists. Conflicted subscription id='" + duplicate.getId() + "'");
        this.duplicate = duplicate;
    }

    public Subscription getDuplicate() {
        return duplicate;
    }
}