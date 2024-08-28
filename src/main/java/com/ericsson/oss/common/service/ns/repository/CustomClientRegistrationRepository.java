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

import com.ericsson.oss.common.service.ns.exception.OAuthRegistrationException;
import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomClientRegistrationRepository implements ClientRegistrationRepository {

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Optional<Credentials> credential = credentialsRepository.findById(UUID.fromString(registrationId));
        if (credential.isPresent()) {
            var credentials = credential.get();
            return ClientRegistration
                    .withRegistrationId(registrationId)
                    .clientId(credentials.getClientId())
                    .clientSecret(credentials.getClientSecret())
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .tokenUri(credentials.getTokenUrl())
                    .build();
        } else {
            throw new OAuthRegistrationException(String.format("Registration ID : %s not found", registrationId));
        }
    }
}
