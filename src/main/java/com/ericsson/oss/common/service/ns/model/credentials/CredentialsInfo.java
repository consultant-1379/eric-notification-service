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

package com.ericsson.oss.common.service.ns.model.credentials;

import java.util.UUID;

public class CredentialsInfo {

    private UUID id;
    private String clientSecret;
    private String clientId;
    private String apiKey;
    private String grantType;
    private String authType;
    private String tokenUrl;
    private String authHeaders;
    private String authTokenRequest;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(final String grantType) {
        this.grantType = grantType;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(final String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getAuthHeaders() {
        return authHeaders;
    }

    public void setAuthHeaders(final String authHeaders) {
        this.authHeaders = authHeaders;
    }

    public String getAuthTokenRequest() {
        return authTokenRequest;
    }

    public void setAuthTokenRequest(String authTokenRequest) {
        this.authTokenRequest = authTokenRequest;
    }
}
