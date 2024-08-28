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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Convert;

import com.ericsson.oss.common.service.ns.configurations.EntityConverter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "credentials")
public class Credentials {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    @Column(nullable = false, name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column
    @Convert(converter = EntityConverter.class)
    private String clientSecret;

    @Column
    private String clientId;

    @Column (nullable = false)
    private String apiKey;

    @Column (nullable = false)
    private String authType;

    @Column
    private String grantType;

    @Column
    private String authHeaders;

    @Column
    private String tokenUrl;

    @Column
    private String authTokenRequest;

    public Credentials() {
    }

    public Credentials(final UUID id, final String clientSecret, final String clientId, final String apiKey,
            final String grantType, final String authType, final String tokenUrl, final String authHeaders, final String authTokenRequest) {
        this.id = id;
        this.clientSecret = clientSecret;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.grantType = grantType;
        this.authType = authType;
        this.tokenUrl = tokenUrl;
        this.authHeaders = authHeaders;
        this.authTokenRequest = authTokenRequest;
    }

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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(final String authType) {
        this.authType = authType;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(final String grantType) {
        this.grantType = grantType;
    }

    public String getAuthHeaders() {
        return authHeaders;
    }

    public void setAuthHeaders(final String authHeaders) {
        this.authHeaders = authHeaders;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(final String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getAuthTokenRequest() {
        return authTokenRequest;
    }

    public void setAuthTokenRequest(String authTokenRequest) {
        this.authTokenRequest = authTokenRequest;
    }
}
