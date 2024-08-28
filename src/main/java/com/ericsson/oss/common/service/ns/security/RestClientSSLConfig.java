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

package com.ericsson.oss.common.service.ns.security;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


@Component
public class RestClientSSLConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientSSLConfig.class);

    @Autowired
    RestTemplate restTemplate;

    public void updateKeyManagerFactory(KeyManagerFactory keyManagerFactory) throws KeyManagementException, NoSuchAlgorithmException {
        LOGGER.info("Updating the KeyManagerFactory");
        var sslContext = SSLContextBuilder.create().build();
        sslContext.init(keyManagerFactory.getKeyManagers(), getTrustManagers(), new SecureRandom());
        final HttpClient httpClient = HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .build();

        final ClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        restTemplate.setRequestFactory(requestFactory);
    }

    public void updateTrustmanagerFactory(TrustManagerFactory trustManagerFactory) {
        LOGGER.info("Updating trust manager factory");
        SSLContext sslContext = null;
        try {
            sslContext = SSLContextBuilder.create().build();

            sslContext.init(getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            final HttpClient httpClient = HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .build();

            final ClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);

            restTemplate.setRequestFactory(requestFactory);
        }
        catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to initialize the keystore:: {} ", e.getMessage());
        } catch (KeyManagementException e) {
            LOGGER.error("Failed to initialize the keystore:: {} ", e.getMessage());
        }
    }

    private KeyManager[] getKeyManagers() {
        return CustomX509KeyManager.getKeyManagerFactory() != null ?
                CustomX509KeyManager.getKeyManagerFactory().getKeyManagers() : null;
    }

    private TrustManager[] getTrustManagers() {
        return CustomX509TrustManager.getTrustManagerFactory() != null ?
                CustomX509TrustManager.getTrustManagerFactory().getTrustManagers() : null;
    }
}
