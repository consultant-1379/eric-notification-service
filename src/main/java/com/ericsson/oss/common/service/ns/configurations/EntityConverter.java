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

package com.ericsson.oss.common.service.ns.configurations;

import com.ericsson.oss.common.service.ns.business.credentialmanager.api.CredentialsManagerService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class EntityConverter implements AttributeConverter<String, String> {

    @Autowired
    private CredentialsManagerService credentialsManagerService;

    @Override
    public String convertToDatabaseColumn(String data) {
        return credentialsManagerService.encryptData(data);
    }

    @Override
    public String convertToEntityAttribute(String data) {
        return credentialsManagerService.decryptData(data);
    }
}