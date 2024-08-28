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

package com.ericsson.oss.common.service.ns.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class CustomX509KeyManager implements X509KeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomX509KeyManager.class);

    private X509KeyManager keyManager;
    private static KeyManagerFactory keyManagerFactory;

    @Value("${keystore.pass}")
    private String keyStorePass;

    @Autowired
    private RestClientSSLConfig restClientSSLConfig;

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return keyManager.chooseClientAlias( keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return keyManager.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManager.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManager.getPrivateKey(alias);
    }

    public static KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    public void setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
        this.keyManagerFactory = keyManagerFactory;
    }

    void loadKeyManagerFactory(InputStream is) throws KeyStoreException, CertificateException {
        try {
            var clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            var keyManagerFactoryObj = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            clientKeyStore.load(is, keyStorePass.toCharArray());
            keyManagerFactoryObj.init(clientKeyStore, keyStorePass.toCharArray());
            this.setKeyManagerFactory(keyManagerFactoryObj);
            restClientSSLConfig.updateKeyManagerFactory(keyManagerFactoryObj);
        } catch (IOException e) {
            LOGGER.error("Failed to load Certificate for KeyStore:: {} ", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to get KeyManagerFactory instance:: {} ", e.getMessage());
        } catch (UnrecoverableKeyException | KeyManagementException e) {
            LOGGER.error("Failed to initialize the keystore:: {} ", e.getMessage());
        }
        LOGGER.info("Client certificate added and accepted successfully");
    }
}