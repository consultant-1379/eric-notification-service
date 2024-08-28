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

package com.ericsson.oss.common.service.ns.business.credentialmanager;

import com.ericsson.oss.common.service.ns.business.credentialmanager.api.CredentialsManagerService;
import com.ericsson.oss.common.service.ns.util.ResponseUtility;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Profile({"prod"})
@Service
public class CredentialsManagerServiceImpl implements CredentialsManagerService {

    @Value("${eric-oss-notification-service.eso-security.host}")
    private String esoSecurityHost;

    @Value("${eric-oss-notification-service.eso-security.retry.attempts}")
    private int retryAttempts;

    @Value("${eric-oss-notification-service.eso-security.retry.delay}")
    private int delay;

    @Value("${eric-eo-credential-manager.self-service.url}")
    private String selfServiceURL;

    @Value("${eric-eo-credential-manager.srm.url}")
    private String srmURL;


    private static final String CONNECTION_PROPERTIES_PARAMETER = "ConnectionProperties";
    private static final String CONNECTION_PROPERTIES_KEY = "connectionProperties";
    private static final String MODULE_PARAMETER = "module";

    private static final Logger LOG = LoggerFactory.getLogger(CredentialsManagerServiceImpl.class);

    @Override
    public String encryptData(String data) {
        String responseData = StringUtils.EMPTY;
        ResponseEntity<Map> response = ResponseUtility.getResponseEntity(WebClient.create(esoSecurityHost),
                UriComponentsBuilder.fromUriString(getCredentialsManagerSelfServiceUrl()).
                        queryParam(CONNECTION_PROPERTIES_PARAMETER, URLEncoder.encode(data, StandardCharsets.UTF_8)).build().toUriString(),
                Map.class, retryAttempts, delay);
        if (Objects.nonNull(response) && HttpStatus.OK.equals(response.getStatusCode())) {
            responseData = Optional.ofNullable(response.getBody())
                    .map(body -> String.valueOf(body.get(CONNECTION_PROPERTIES_KEY))).orElse(StringUtils.EMPTY);
            LOG.debug("encryptData() successful, encrypted string is: {}", responseData);
        } else {
            LOG.error("encryptData() failed with response {}", response);
        }
        return responseData;
    }

    @Override
    public String decryptData(String data) {
        String responseData = StringUtils.EMPTY;
        ResponseEntity<Map> response = ResponseUtility.getResponseEntity(WebClient.create(esoSecurityHost),
                UriComponentsBuilder.fromUriString(getCredentialsManagerSRMUrl()).
                        queryParam(MODULE_PARAMETER, URLEncoder.encode(data, StandardCharsets.UTF_8)).build(true).toUriString(), Map.class
                , retryAttempts, delay);
        if (Objects.nonNull(response) && HttpStatus.OK.equals(response.getStatusCode())) {
            responseData = URLDecoder.decode(Optional.ofNullable(response.getBody())
                    .map(body -> String.valueOf(body.get(CONNECTION_PROPERTIES_KEY))).orElse(StringUtils.EMPTY), StandardCharsets.UTF_8);
            LOG.debug("decryptData() successful, encrypted string is: {}", responseData);
        } else {
            LOG.error("decryptData() failed with response {}", response);
        }
        return responseData;
    }


    public String getCredentialsManagerSelfServiceUrl() {
        return selfServiceURL;
    }

    public String getCredentialsManagerSRMUrl() {
        return srmURL;
    }

}
