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

import com.ericsson.oss.common.service.ns.model.credentials.Credentials;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;

/**
 * Converter class to map Credentials records into CredentialsInfo and CredentialsInfo into Credentials.
 */
public class CredentialsMapper {

    // Utility classes should not have public constructors
    private CredentialsMapper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Convert Credentials into CredentialsInfo
     * @param entity The Credentials entity to be converted
     * @return The returned CredentialsInfo structure
     */
    public static CredentialsInfo convertToDto(Credentials entity) {
        if (entity == null) {
            return null;
        }
        var dto = new CredentialsInfo();
        dto.setId(entity.getId());
        dto.setApiKey(entity.getApiKey());
        dto.setAuthType(entity.getAuthType());
        dto.setClientSecret(entity.getClientSecret());
        dto.setClientId(entity.getClientId());
        dto.setGrantType(entity.getGrantType());
        dto.setTokenUrl(entity.getTokenUrl());
        dto.setAuthHeaders(entity.getAuthHeaders());
        dto.setAuthTokenRequest(entity.getAuthTokenRequest());
        return dto;
    }

    /**
     * Convert an CredentialsInfo into a Credentials record.
     * @param dto The CredentialsInfo to be converted
     * @return The returned Credentials record
     */
    public static Credentials convertToEntity(CredentialsInfo dto) {
        if (dto == null) {
            return null;
        }
        var entity = new Credentials();
        entity.setApiKey(dto.getApiKey());
        entity.setAuthType(dto.getAuthType());
        entity.setClientSecret(dto.getClientSecret());
        entity.setClientId(dto.getClientId());
        entity.setGrantType(dto.getGrantType());
        entity.setTokenUrl(dto.getTokenUrl());
        entity.setAuthHeaders(dto.getAuthHeaders());
        entity.setAuthTokenRequest(dto.getAuthTokenRequest());
        return entity;
    }
}
