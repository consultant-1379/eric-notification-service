/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

package com.ericsson.oss.common.service.ns.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

public class ResponseUtility {

    private ResponseUtility() {
       throw new IllegalStateException("ResponseUtility class");
    }

    public static <T> ResponseEntity<T> getResponseEntity(WebClient webClient, String uri, Class<T> responseType, int retryAttempts,
                                                          int delay) {
        return webClient.get().uri(uri).retrieve().toEntity(responseType).retryWhen(Retry.fixedDelay(retryAttempts,
                        Duration.ofSeconds(delay))
                .filter(ResponseUtility::isRetryException)).block();
    }

    public static boolean isRetryException(Throwable ex) {
        var result = false;

        if (ex instanceof ConnectException) {
            result = true;
        } else if (ex instanceof WebClientResponseException) {
            WebClientResponseException response = (WebClientResponseException) ex;
            if (HttpStatus.REQUEST_TIMEOUT.value() == response.getRawStatusCode() || response.getStatusCode().is5xxServerError()) {
                result = true;
            }
        } else if (ex instanceof SocketTimeoutException) {
            result = true;
        }
        return result;
    }
}
