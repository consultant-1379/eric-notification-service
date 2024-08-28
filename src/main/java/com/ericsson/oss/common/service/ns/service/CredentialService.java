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

package com.ericsson.oss.common.service.ns.service;

import com.ericsson.oss.common.service.ns.model.credentials.CredentialsInfo;

public interface CredentialService {
     /**
      * Fetch the details from the external systems.
      * Validate that all credentials needed are correct and present
      * @param apiKey - used to fetch the credential details from external system
      * @return credentials
      */
     CredentialsInfo getCredentials(String apiKey, String address);
}
