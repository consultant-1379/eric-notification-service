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

import com.ericsson.oss.common.service.ns.exception.IncorrectJsonException;
import com.ericsson.oss.common.service.ns.exception.MissingArgumentException;
import com.ericsson.oss.common.service.ns.exception.IncorrectHttpHeaderException;
import com.ericsson.oss.common.service.ns.infrastructure.configurations.WebClientConfig;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialProperties;
import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.util.Constants;
import com.ericsson.oss.common.service.ns.util.ResponseUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class CredentialServiceImpl implements CredentialService {

    @Value("${external.credentials.url}")
    private String url;

    @Value("${external.credentials.retry.attempts}")
    private int retryAttempts;

    @Value("${external.credentials.retry.delay}")
    private int delay;

    @Autowired
    private WebClientConfig webClientConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public CredentialsInfo getCredentials(String apiKey, String address) {
        String uuid = getApiKey(apiKey);

        var webClient = webClientConfig.createWebClient();
        String uri = normaliseUrl(url, uuid);
        CredentialProperties response = null;

        try {
            response = webClient
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(CredentialProperties.class)
                    .retryWhen(Retry.fixedDelay(retryAttempts, Duration.ofSeconds(delay))
                            .filter(ResponseUtility::isRetryException))
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalArgumentException(String.format("Failed to get credential information: %s", ex));
        }

        if (response == null) {
            throw new IllegalArgumentException(String.format("response to get credentials was null for API KEY %s", apiKey));
        } else if (response.getConnectionProperties().isEmpty()) {
            throw new IllegalArgumentException(String.format("no connections properties found for API KEY: %s", apiKey));
        }

        var credentialsInfo = mapAndValidateCredentials(response, address);
        credentialsInfo.setApiKey(uuid);

        return credentialsInfo;
    }

    private String getApiKey(String apiKey) {
        if (apiKey.isBlank()) {
            throw new IncorrectHttpHeaderException("X-API-KEY header was set but is blank");
        }
        String uuid;
        try {
            uuid = decodeBase64(apiKey);
        } catch (IllegalArgumentException e) {
            throw new IncorrectHttpHeaderException("X-API-KEY header is not in base64 format");
        }
        return uuid;
    }

    private String normaliseUrl(String url, String uuid) {
        return url.endsWith("/") ? url + uuid: url + "/" + uuid;
    }

    private String decodeBase64(String apikey) {
        byte[] resultBytes = Base64.getDecoder().decode(apikey);
        return new String(resultBytes);
    }

    private CredentialsInfo mapAndValidateCredentials(CredentialProperties credentialProperties, String address) {

        var connectionProperties = credentialProperties.getConnectionProperties().get(0);

        String type = connectionProperties.getAuthType();

        var credentialsInfo = new CredentialsInfo();

        if (Constants.OAUTH2.equalsIgnoreCase(type) || Constants.OAUTH2_0.equalsIgnoreCase(type)) {

            credentialsInfo.setClientId(connectionProperties.getClientId());
            credentialsInfo.setClientSecret(connectionProperties.getClientSecret());
            credentialsInfo.setGrantType(connectionProperties.getGrantType());
            credentialsInfo.setTokenUrl(connectionProperties.getAuthUrl());
            credentialsInfo.setAuthHeaders(connectionProperties.getAuthHeaders());
            credentialsInfo.setAuthType(connectionProperties.getAuthType());
            credentialsInfo.setAuthTokenRequest(connectionProperties.getAuthTokenRequest());

            validateOauthCredentials(credentialsInfo);
            validateOauthTlsProtocol(credentialsInfo, address);
            validateCredentialsInfo(credentialsInfo);

        } else {
            throw new MissingArgumentException(String.format("AUTH_TYPE value '%s' is null or not supported. " +
                    "Only Oauth2 or Oauth2.0 values are supported", type));
        }

        return credentialsInfo;

    }

    private void validateCredentialsInfo(CredentialsInfo credentialsInfo) {
        if (StringUtils.isEmpty(credentialsInfo.getAuthHeaders())) {
            throw new IncorrectHttpHeaderException("Content-Type header is not set.");
        }
        Optional<String> contentType = getContentType(credentialsInfo.getAuthHeaders());
        validateContentType(contentType);
    }

    private void validateContentType(Optional<String> contentType) {
        if (contentType.isEmpty()) {
            throw new IncorrectHttpHeaderException("Content-Type header is not set.");
        } else if (!MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(contentType.get()) && !MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType.get())) {
            throw new IncorrectHttpHeaderException(String.format("Content-Type is not supported %s", contentType.get()));
        }
    }

    public Optional<String> getContentType(String content) {
        try {
            Map<String, String> headers = objectMapper.readValue(content, Map.class);
            return Optional.ofNullable(headers.get(HttpHeaders.CONTENT_TYPE));
        } catch (JsonProcessingException ex) {
            throw new IncorrectJsonException(ex.getMessage());
        }
    }

    private void validateOauthTlsProtocol(CredentialsInfo credentialsInfo, String address) {
        if (!credentialsInfo.getTokenUrl().toLowerCase().startsWith("https")) {
            throw new RuntimeException(String.format("The token url is not secured under TLS protocol '%s'. " +
                            "This must contain https protocol for oauth2", credentialsInfo.getTokenUrl()));
        }

        if (address != null && !address.toLowerCase().startsWith("https")) {
            throw new RuntimeException(String.format("The notification address is not secured under TLS protocol " +
                    "'%s'. This must contain https protocol for oauth2", address));
        }

    }

    private void validateOauthCredentials(CredentialsInfo credentialsInfo) {
        var result = false;
        ArrayList<String> emptyFields = new ArrayList<>();
        if (Strings.isNullOrEmpty(credentialsInfo.getClientId())) {
            result = true;
            emptyFields.add(Constants.CLIENT_ID);
        }
        if (Strings.isNullOrEmpty(credentialsInfo.getClientSecret())) {
            result = true;
            emptyFields.add(Constants.CLIENT_SECRET);
        }
        if (Strings.isNullOrEmpty(credentialsInfo.getAuthHeaders())) {
            result = true;
            emptyFields.add(Constants.AUTH_HEADERS);
        }
        if (Strings.isNullOrEmpty(credentialsInfo.getGrantType())) {
            result = true;
            emptyFields.add(Constants.GRANT_TYPE);
        }
        if (Strings.isNullOrEmpty(credentialsInfo.getTokenUrl())) {
            result = true;
            emptyFields.add(Constants.AUTH_URL);
        }

        if (result) {
            throw new MissingArgumentException(String.format("fields required for oauth2 are empty or missing: %s",
                    emptyFields));
        }
    }
}
