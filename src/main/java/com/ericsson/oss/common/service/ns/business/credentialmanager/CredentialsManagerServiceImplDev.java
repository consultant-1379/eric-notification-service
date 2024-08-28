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

package com.ericsson.oss.common.service.ns.business.credentialmanager;

import com.ericsson.oss.common.service.ns.business.credentialmanager.api.CredentialsManagerService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"dev", "test", "test-kafka"})
@Service
public class CredentialsManagerServiceImplDev implements CredentialsManagerService {
    @Override
    public String encryptData(String data) {
        return data;
    }

    @Override
    public String decryptData(String data) {
        return data;
    }
}
