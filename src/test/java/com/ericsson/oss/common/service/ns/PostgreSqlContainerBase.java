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

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The postgres version used in the tests should be same version
 * used by eric-data-document-database-pg version defined in
 * eo-integration-charts' charts/eric-eo/requirements.yaml.
 * The current postgres version used here comes from
 * https://gerrit.ericsson.se/plugins/gitiles/adp-gs/adp-gs-postgres/+/v4.2.0+48/pg-ha/3pp/postgres/docker/Dockerfile
 */
public abstract class PostgreSqlContainerBase {

    static final String IMAGE_VERSION = "postgres:13.6";
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(IMAGE_VERSION).withReuse(true);
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> String.format(
            "jdbc:tc:%s:///test?TC_DAEMON=true;TC_TMPFS=/testtmpfs:rw",
            IMAGE_VERSION.replace(":", "ql:")));
    }

}
