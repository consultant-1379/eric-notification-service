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

package com.ericsson.oss.common.service.ns.infrastructure.configurations;

import com.ericsson.oss.common.service.ns.security.CustomX509KeyManager;
import com.ericsson.oss.common.service.ns.security.CustomX509TrustManager;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import javax.net.ssl.SSLException;

@Configuration
public class WebClientConfig {

    @Value("${restClient.connectionTimeout}")
    private int connectionTimeout;

    @Value("${restClient.readTimeout}")
    private int readTimeout;

    @Autowired
    private CustomX509TrustManager trustManager;

    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    private CustomX509KeyManager keyManager;

    /**
     * This method add SSL context to webClient
     * @return WebClient
     */
    public WebClient createWebClient() {
        var oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        try {
            var sslContext = SslContextBuilder
                    .forClient()
                    .keyManager(keyManager.getKeyManagerFactory())
                    .trustManager(trustManager.getTrustManagerFactory())
                    .build();

            var httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(readTimeout))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout)))
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

            return WebClient.builder()
                    .clientConnector(connector)
                    .apply(oauth2Client.oauth2Configuration())
                    .build();

        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }
}
