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

package com.ericsson.oss.common.service.ns.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "certificates")
public class CertificateConfig {

    private List<Secrets> secrets;

    public List<Secrets> getSecrets() {
        return secrets;
    }

    public void setSecrets(List<Secrets> secrets) {
        this.secrets = secrets;
    }

    @Getter
    @Setter
    public static class Secrets {

        private String certTruststoreName;
        private String certTruststoreField;
        private String certKeystoreName;
        private String certKeystoreField;
    }
}