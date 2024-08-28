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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;

class CredentialsMapperTest {

    @Test
    void shouldMapDtoToEntity() {
        CredentialsInfo dto = new CredentialsInfo();
        dto.setClientId("clientId");
        dto.setTokenUrl("tokenUrl");
        dto.setClientSecret("clientSecret");
        dto.setGrantType("grantType");
        dto.setAuthType("OAuth2");
        dto.setApiKey("api-key-test");
        dto.setAuthHeaders("authHeaders");

        Credentials entity = CredentialsMapper.convertToEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getAuthHeaders(), entity.getAuthHeaders());
        assertEquals(dto.getApiKey(), entity.getApiKey());
        assertEquals(dto.getClientSecret(), entity.getClientSecret());
        assertEquals(dto.getClientId(), entity.getClientId());
        assertEquals(dto.getAuthType(), entity.getAuthType());
        assertEquals(dto.getGrantType(), entity.getGrantType());
        assertEquals(dto.getTokenUrl(), entity.getTokenUrl());
    }

    @Test
    void shouldMapEntityToDto() {
        Credentials entity = new Credentials();
        entity.setId(UUID.randomUUID());
        entity.setClientId("clientId");
        entity.setTokenUrl("tokenUrl");
        entity.setClientSecret("clientSecret");
        entity.setGrantType("grantType");
        entity.setAuthType("OAuth2");
        entity.setApiKey("api-key-test");
        entity.setAuthHeaders("authHeaders");

        CredentialsInfo dto = CredentialsMapper.convertToDto(entity);

        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getClientId(), dto.getClientId());
        assertEquals(entity.getAuthHeaders(), dto.getAuthHeaders());
        assertEquals(entity.getApiKey(), dto.getApiKey());
        assertEquals(entity.getAuthType(), dto.getAuthType());
        assertEquals(entity.getGrantType(), dto.getGrantType());
        assertEquals(entity.getTokenUrl(), dto.getTokenUrl());
        assertEquals(entity.getClientSecret(), dto.getClientSecret());
    }

    @Test
    void shouldBeNullIfNullObjectProvided() {
        assertNull(CredentialsMapper.convertToDto(null));
        assertNull(CredentialsMapper.convertToEntity(null));
    }
}