/*******************************************************************************
 * COPYRIGHT Ericsson 2020
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
package com.ericsson.oss.common.service.ns;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.Secret;

@Configuration
@ConditionalOnKubernetesEnabled
public class DatabaseConfiguration {
    @Value("${spring.datasource.url}")
    String url;
    @Value("${spring.datasource.secret.name}")
    String secretName;
    @Value("${spring.datasource.secret.user}")
    String secretUser;
    @Value("${spring.datasource.secret.password}")
    String secretPassword;
    @Bean
    public DataSource getDataSource()
    {
        KubernetesClient client = new DefaultKubernetesClient();
        Secret secret = client.secrets().withName(secretName).get();
        String decodedUser = new String(Base64.getDecoder().decode(secret.getData().get(secretUser)), StandardCharsets.UTF_8);
        String decodedPassword = new String(Base64.getDecoder().decode(secret.getData().get(secretPassword)), StandardCharsets.UTF_8);

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(url);
        dataSourceBuilder.username(decodedUser);
        dataSourceBuilder.password(decodedPassword);

        return dataSourceBuilder.build();
    }
}
