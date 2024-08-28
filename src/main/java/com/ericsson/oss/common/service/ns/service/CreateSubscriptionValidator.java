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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionFilter;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.exception.DuplicateSubscriptionException;
import com.ericsson.oss.common.service.ns.exception.MissingArgumentException;
import com.ericsson.oss.common.service.ns.filter.RsqlFilter;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.repository.SubscriptionBusinessKey;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;
import com.ericsson.oss.common.service.ns.util.Utils;
import com.google.common.base.Strings;
import com.jayway.jsonpath.InvalidJsonException;

/**
 * Class to validate subscription creation
 */
@Configuration
public class CreateSubscriptionValidator {

    private static final String COMMA = ",";
    private static final String DOT = ".";
    private static final String OPEN_SQUARE_BRACKET = "[";
    private static final String CLOSED_SQUARE_BRACKET = "]";
    private static final String STAR = "*";

    private static final Pattern JSON_NAME_PATTERN = Pattern.compile("[a-z][a-zA-Z0-9_-]*(\\[([0-9]+|\\*)])?"); // Pattern for JSON name (with
    // optional array entry)

    /**
     * Check if mandatory fields (other the ones already checked in eric-notification-service-api library) are missing. In case of
     * missing fields, a MissingArgumentException is thrown
     *
     * @param subscription The subscription request to be checked
     */
    public void validateMandatoryFields(NsSubscriptionRequest subscription) {
        if (subscription.getTenant() == null) {
            throw new MissingArgumentException("Field 'tenant' must not be null. If no tenant expected, set to empty string");
        }
        if (CollectionUtils.isEmpty(subscription.getSubscriptionFilter())) {
            throw new MissingArgumentException("Mandatory field 'subscriptionFilter' missing or empty");
        }
    }

    /**
     * Check if the "address" field contains a valid URL. In case of invalid field, an IllegalArgumentException is thrown
     *
     * @param address The content of the address field
     */
    public void validateUrl(String address) {
        if (!Utils.isURI(address)) {
            throw new IllegalArgumentException("Field 'address' must to be a valid URL");
        }
    }

    /**
     * Validate the subscriptionFilters. In case of errors, IllegalArgumentException is thrown
     * @param {@link SubscriptionFilter} The list of subscription filters in the request
     */
    public void validateSubscriptionFilters(List<NsSubscriptionFilter> subscriptionFilter) {
        subscriptionFilter.forEach(s -> {
            if (s.getFilterCriteria() != null && !RsqlFilter.isValid(s.getFilterCriteria())) {
                throw new IllegalArgumentException("field 'filterCriteria' has invalid RSQL format. filterCriteria=" + s.getFilterCriteria());
            }
            if (s.getFields() != null && !isValidJsonKeys(s.getFields())) {
                throw new IllegalArgumentException("field 'fields' contains invalid json keys. fields=" + s.getFields());
            }
        });
    }

    /**
     * Check for subscriptions in the database with the same key attributes: address + tenant + eventType + filterCriteria.
     * If a record with the same hash (obtained concatenating the attributes above) is found, a DuplicateSubscriptionException is
     * thrown.
     * @param {@Link Subscription} The subscription request to be checked
     */
    public void validateIfDuplicateExists(NsSubscriptionRequest subscription, SubscriptionRepository subscriptionRepository) {
        Map<String, String> filterCriterias = subscription.getSubscriptionFilter().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getEventType(), v.getFilterCriteria()), HashMap::putAll);
        filterCriterias.forEach((eventType, filterCriteria) -> {
            SubscriptionBusinessKey key = SubscriptionBusinessKey.builder()
                    .address(subscription.getAddress())
                    .tenant(subscription.getTenant())
                    .eventType(eventType)
                    .filterCriteria(filterCriteria)
                    .build();
            List<Subscription> duplicate = subscriptionRepository.findByHash(key.getHash());
            if (!duplicate.isEmpty()) {
                throw new DuplicateSubscriptionException(duplicate.get(0));
            }
        });
    }

    /**
     * Check if mandatory credential fields are missing. In case of missing fields, a MissingArgumentException is thrown
     * @param credentials The credentials to be checked
     */
    public void validateMandatoryCredentialFields(CredentialsInfo credentials) {
        if (Strings.isNullOrEmpty(credentials.getApiKey())) {
            throw new MissingArgumentException("Mandatory apiKey missing");
        }
        if (Strings.isNullOrEmpty(credentials.getAuthType())) {
            throw new MissingArgumentException("Mandatory authType missing");
        }
    }

    /**
     * Check the "fields" attribute for a valid comma-saparated list of Json keys (valid json attribute names). It's allowed the
     * format name1.name2[*].name3 to indicate that the field name3 of the array name2 (under the structure name1) shall be
     * reported for any values of the name2 indexes.
     * @param fields The content of the "fields" attribute
     * @return True if ok, false is there are errors in the "fields" attribute
     */
    private boolean isValidJsonKeys(String fields) {
        if (fields.isEmpty()) {
            return false;
        }
        String[] jsonFieldPaths = fields.contains(COMMA) ? fields.trim().split(COMMA) : new String[]{fields.trim()};
        try {
            validateKeys(jsonFieldPaths);
        } catch (InvalidJsonException e) {
            return false;
        }
        return true;
    }

    /**
     * Validate the Json names found inside the "fields" attribute. Throws InvalidJsonException if any errors.
     * @param paths The list attributes included in "fields"
     * @throws InvalidJsonException if it is not possible to create json object with the provided key
     */
    private void validateKeys(String[] paths) {
        for (String fullPath : paths) {
            String[] pathParts = fullPath.contains(DOT) ? fullPath.trim().split("\\.") : new String[]{fullPath.trim()};
            for (String key : pathParts) {
                if (key.isBlank()) {
                    throw new InvalidJsonException("json key must not be empty");
                }
                if (!JSON_NAME_PATTERN.matcher(key).matches()) {
                    throw new InvalidJsonException("invalid field " + key);
                }
                String actualKey = key.replace(OPEN_SQUARE_BRACKET, "\\[");
                actualKey = actualKey.replace(CLOSED_SQUARE_BRACKET, "\\]");
                actualKey = actualKey.replace(STAR, "[0-9]*");
                try {
                    Pattern.compile(actualKey);
                } catch (PatternSyntaxException exception) {
                    throw new InvalidJsonException("invalid field " + key);
                }
            }
        }
    }

}

