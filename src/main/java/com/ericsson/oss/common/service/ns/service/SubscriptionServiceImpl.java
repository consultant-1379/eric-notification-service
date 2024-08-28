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

import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionRequest;
import com.ericsson.oss.common.service.ns.api.model.NsSubscriptionResponse;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.model.subscription.Subscription;
import com.ericsson.oss.common.service.ns.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.ericsson.oss.common.service.ns.service.SubscriptionMapper.convertToDto;
import static com.ericsson.oss.common.service.ns.service.SubscriptionMapper.convertToEntity;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    private final CreateSubscriptionValidator createSubscriptionValidator;

    public SubscriptionServiceImpl(final SubscriptionRepository subscriptionRepository,
                                   final CreateSubscriptionValidator createSubscriptionValidator) {
        this.subscriptionRepository = subscriptionRepository;
        this.createSubscriptionValidator = createSubscriptionValidator;
    }

    @Override
    @Transactional
    public NsSubscriptionResponse save(final NsSubscriptionRequest subscription) {
        subscriptionValidation(subscription);
        var entity = subscriptionRepository.save(convertToEntity(subscription));
        return convertToDto(entity);
    }

    @Override
    @Transactional
    public NsSubscriptionResponse save(final NsSubscriptionRequest subscription, final CredentialsInfo credentialsInfo) {
        credentialValidation(credentialsInfo);
        var credentials = CredentialsMapper.convertToEntity(credentialsInfo);
        subscriptionValidation(subscription);
        Subscription entity = convertToEntity(subscription);
        entity.setCredentials(credentials);
        entity = subscriptionRepository.save(entity);
        return convertToDto(entity);
    }

    @Override
    public void delete(final UUID id) {
        try {
            subscriptionRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException(id.toString());
        }
    }

    @Override
    @Transactional
    public NsSubscriptionResponse get(final UUID id) {
        Optional<Subscription> subscriptionRecord = subscriptionRepository.findById(id);
        if (!subscriptionRecord.isPresent()) {
            throw new NoSuchElementException(id.toString());
        }
        return convertToDto(subscriptionRecord.get());
    }

    @Override
    @Transactional
    public List<NsSubscriptionResponse> getAll() {
        List<Subscription> subscriptionsDao = subscriptionRepository.findAll();
        List<NsSubscriptionResponse> subscriptions = new ArrayList<>();
        for (Subscription subscription : subscriptionsDao) {
            subscriptions.add(convertToDto(subscription));
        }
        return subscriptions;
    }

    private void subscriptionValidation(final NsSubscriptionRequest subscription) {
        createSubscriptionValidator.validateMandatoryFields(subscription);
        createSubscriptionValidator.validateUrl(subscription.getAddress());
        createSubscriptionValidator.validateSubscriptionFilters(subscription.getSubscriptionFilter());
        createSubscriptionValidator.validateIfDuplicateExists(subscription, subscriptionRepository);
    }

    private void credentialValidation(final CredentialsInfo credentials) {
        createSubscriptionValidator.validateMandatoryCredentialFields(credentials);
    }
}