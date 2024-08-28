/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsManagerServiceImplDevTest {

    private static final String ENCRYPTED_DATA = "VEVTVA==";
    private static final String DECRYPTED_DATA = "Test";

    @InjectMocks
    CredentialsManagerServiceImplDev credentialsManagerServiceImplDev ;

    @Test
    public void encryptData() {
        String encryptedString = credentialsManagerServiceImplDev.encryptData(ENCRYPTED_DATA);
        assertEquals(ENCRYPTED_DATA, encryptedString);
    }

    @Test
    public void decryptData() {
        String decryptedString = credentialsManagerServiceImplDev.encryptData(DECRYPTED_DATA);
        assertEquals(DECRYPTED_DATA, decryptedString);
    }

}