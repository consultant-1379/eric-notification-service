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

import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import com.ericsson.oss.common.service.ns.model.dispatch.Dispatch;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * Convert a set of SQL results from the database dispatch table into a list of Dispatch records.
 */
public class DispatchResultSetExtractor implements ResultSetExtractor<List<Dispatch>> {

  /**
   * Extract the information from the SQL result set.
   * @param rs The result set to be converted
   * @return The list of Dispatch records
   * @throws SQLException
   */
  @Override
  public List<Dispatch> extractData(ResultSet rs) throws SQLException {
    return map(rs);
  }

  /**
   * Map each record of the SQL ResultSet to a Dispatch record and add that to the list to be returned.
   * @param rs The SQL result set
   * @return The list of Dispatch records to be returned.
   * @throws SQLException
   */
  private List<Dispatch> map(ResultSet rs) throws SQLException {
    List<Dispatch> result = new ArrayList<>();
    while (rs.next()) {
      var eventId = rs.getString("event_id");
      var address = rs.getString("address");
      Integer hash = rs.getInt("hash");
      var payload = rs.getString("payload");
      var oauthClientRegistrationId = rs.getString("oauth_registration_id");
      var authHeader = rs.getString("auth_headers");
      var clientId = rs.getString("client_id");
      var clientSecret = rs.getString("client_secret");
      var grantType = rs.getString("grant_type");
      var authTokenRequest = rs.getString("auth_token_request");
      var tokenUrl = rs.getString("token_url");
      CredentialsInfo credentialsInfo = getCredentialsInfo(clientId, clientSecret, grantType, authTokenRequest, tokenUrl);
      result.add(new Dispatch(eventId, address, hash, payload, oauthClientRegistrationId, authHeader, credentialsInfo));
    }
    return result;
  }

  private CredentialsInfo getCredentialsInfo(String clientId, String clientSecret, String grantType, String authTokenRequest, String tokenUrl) {
    CredentialsInfo credentialsInfo = new CredentialsInfo();
    credentialsInfo.setClientId(clientId);
    credentialsInfo.setClientSecret(clientSecret);
    credentialsInfo.setGrantType(grantType);
    credentialsInfo.setAuthTokenRequest(authTokenRequest);
    credentialsInfo.setTokenUrl(tokenUrl);

    return credentialsInfo;
  }

}
