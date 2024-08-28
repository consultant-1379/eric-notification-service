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

package com.ericsson.oss.common.service.ns.model.dispatch;

import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The current_dispatch table model.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dispatch {
  /**
   * The id of the event whose relevant notification is being dispatched.
   */
  private String eventId;
  /**
   * The destination address of the delivery.
   */
  private String address;
  /**
   * A hash created from the payload. If it's 0, payload is empty as the event payload shall be used in the notification.
   */
  private Integer hash;
  /**
   * The payload of the notification.
   */
  private String payload;
  /***
   * The unique registrationId for oauth2 flow
   */
  private String oauthRegistrationId;
  /***
   * Specifies the content-type
   */
  private String authHeaders;
  /***
   * The CredentialsInfo to be used for OAuth requests
   */
  private CredentialsInfo credentialsInfo;

  /**
   * Return the value of this record key. It's the concatenation of the eventId, address and hash value.
   * @return The key
   */
  public String getKey() {
    return eventId + address + hash;
  }
}
