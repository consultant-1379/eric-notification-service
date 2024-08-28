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

package com.ericsson.oss.common.service.ns.repository;

import static java.lang.String.format;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTableWhere;

import com.ericsson.oss.common.service.ns.PostgreSqlContainerBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = NONE)
class FlywayTest extends PostgreSqlContainerBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(strings = {"subscription", "subscription_event_type", "current_dispatch", "credentials"})
    void shouldTablesExist(String tableUnderTest) {
        // given
        String whereClause = format("tablename = '%s'", tableUnderTest);

        // when
        int total = countRowsInTableWhere(jdbcTemplate, "pg_tables", whereClause);

        then(total)
            .as("flyway tables have been created successfully")
            .isEqualTo(1);
    }

}
