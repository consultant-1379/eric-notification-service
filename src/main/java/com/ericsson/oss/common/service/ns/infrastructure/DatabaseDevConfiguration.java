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

package com.ericsson.oss.common.service.ns.infrastructure;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.Properties;

@Profile("dev")
@Configuration
public class DatabaseDevConfiguration {
    private static final String POSTGRES = "postgres";
    private static final String POSTGRES_IMAGE = "postgres:13.6";

    @Bean
    @Primary
    @DependsOn("embeddedPostgresDS")
    public DataSource embeddedPostgresConnection(EmbeddedPostgres postgresDatabase) {
        var props = new Properties();
        props.setProperty("stringtype", "unspecified");
        String jdbcUrl = postgresDatabase.getJdbcUrl(POSTGRES);
        var driverManagerDataSource = new DriverManagerDataSource(jdbcUrl, props);
        driverManagerDataSource.setUsername(POSTGRES);
        driverManagerDataSource.setPassword(POSTGRES);
        return driverManagerDataSource;
    }

    @Bean
    public EmbeddedPostgres embeddedPostgresDS() throws IOException {
        return EmbeddedPostgres.builder()
                .setImage(DockerImageName.parse(POSTGRES_IMAGE))
                .setDatabaseName(POSTGRES)
                .start();
    }
}

