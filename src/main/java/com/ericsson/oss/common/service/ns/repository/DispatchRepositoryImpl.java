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

import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DispatchRepositoryImpl implements DispatchRepository {

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void save(Dispatch dispatchRecord) {
    String dispatchInsert;
    if (dispatchRecord.getHash() == 0) {
      dispatchInsert = "insert into current_dispatch(event_id, address, hash, oauth_registration_id, auth_headers) values(:event_id,:address,:hash,:oauth_registration_id, :auth_headers)";
    } else {
      dispatchInsert = "insert into current_dispatch(event_id, address, hash, payload, oauth_registration_id, auth_headers) values(:event_id,:address,:hash,:payload,:oauth_registration_id, :auth_headers)";
    }
    namedParameterJdbcTemplate.update(dispatchInsert, mapDispatchParams(dispatchRecord));
  }

  @Override
  @Transactional
  public void save(List<Dispatch> dispatchRecords) {
    var dispatchInsert = new StringBuilder("");
    for (Dispatch dispatchRecord: dispatchRecords) {
      if (dispatchRecord.getHash() == 0) {
        dispatchInsert.append("insert into current_dispatch(event_id, address, hash, oauth_registration_id, auth_headers) values('");
        dispatchInsert.append(dispatchRecord.getEventId());
        dispatchInsert.append("','");
        dispatchInsert.append(dispatchRecord.getAddress());
        dispatchInsert.append("',");
        dispatchInsert.append(dispatchRecord.getHash());
        dispatchInsert.append(",'");
        dispatchInsert.append(dispatchRecord.getOauthRegistrationId());
        dispatchInsert.append("','");
        dispatchInsert.append(dispatchRecord.getAuthHeaders());
        dispatchInsert.append("');");
      } else {
        dispatchInsert.append("insert into current_dispatch(event_id, address, hash, payload, oauth_registration_id, auth_headers) values('");
        dispatchInsert.append(dispatchRecord.getEventId());
        dispatchInsert.append("','");
        dispatchInsert.append(dispatchRecord.getAddress());
        dispatchInsert.append("',");
        dispatchInsert.append(dispatchRecord.getHash());
        dispatchInsert.append(",'");
        dispatchInsert.append(dispatchRecord.getPayload());
        dispatchInsert.append("','");
        dispatchInsert.append(dispatchRecord.getOauthRegistrationId());
        dispatchInsert.append("','");
        dispatchInsert.append(dispatchRecord.getAuthHeaders());
        dispatchInsert.append("');");
      }
    }
    dispatchInsert.append("commit;");
    jdbcTemplate.execute(dispatchInsert.toString());
  }


  @Override
  @Transactional
  public int delete(String eventId, String address, Integer hash) {
    final var dispatchDelete = "DELETE FROM current_dispatch WHERE event_id=:event_id AND address=:address AND hash=:hash";
    return namedParameterJdbcTemplate.update(dispatchDelete, mapDispatchParams(eventId, address, hash));
  }

  @Override
  public List<Dispatch> findAll(String eventId) {
    return namedParameterJdbcTemplate.query(
            "select d.event_id, d.address, d.hash, d.payload, d.oauth_registration_id, d.auth_headers, cr.client_id, cr.client_secret, cr.grant_type, cr.auth_token_request, cr.token_url FROM current_dispatch d LEFT join credentials cr ON d.oauth_registration_id = cr.id::text WHERE d.event_id='" + eventId + "'",
            new DispatchResultSetExtractor());
  }

  private MapSqlParameterSource mapDispatchParams(Dispatch dispatchRecord) {
    var mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("event_id", dispatchRecord.getEventId());
    mapSqlParameterSource.addValue("address", dispatchRecord.getAddress());
    mapSqlParameterSource.addValue("hash", dispatchRecord.getHash());
    if (dispatchRecord.getHash() != 0) {
      mapSqlParameterSource.addValue("payload", dispatchRecord.getPayload());
    }
    mapSqlParameterSource.addValue("oauth_registration_id", dispatchRecord.getOauthRegistrationId());
    mapSqlParameterSource.addValue("auth_headers", dispatchRecord.getAuthHeaders());
    return mapSqlParameterSource;
  }

  private MapSqlParameterSource mapDispatchParams(String eventId, String address, Integer hash) {
    var mapSqlParameterSource = new MapSqlParameterSource();
    mapSqlParameterSource.addValue("event_id", eventId);
    mapSqlParameterSource.addValue("address", address);
    mapSqlParameterSource.addValue("hash", hash);
    return mapSqlParameterSource;
  }
}
