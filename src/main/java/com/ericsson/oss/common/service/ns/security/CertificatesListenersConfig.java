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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesEnabled;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;

@Configuration
@ConditionalOnKubernetesEnabled
public class CertificatesListenersConfig {

    @Bean
    public ChangeDetector certificateSecretChangeDetector(AbstractEnvironment environment,
                                                          KubernetesClient client,
                                                          ConfigReloadProperties reloadProperties,
                                                          CustomX509TrustManager trustManager,
                                                          CustomX509KeyManager keyManager) {
        return new CertificateEventChangeDetector(environment, client, reloadProperties, trustManager, keyManager);
    }
}
