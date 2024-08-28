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

import static io.fabric8.kubernetes.client.Watcher.Action.ADDED;
import static io.fabric8.kubernetes.client.Watcher.Action.MODIFIED;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.core.env.AbstractEnvironment;

import com.ericsson.oss.common.service.ns.configurations.CertificateConfig;
import com.google.common.collect.Maps;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;

public class CertificateEventChangeDetector extends ChangeDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateEventChangeDetector.class);
    private static final String X509_INSTANCE = "X.509";

    private final CustomX509TrustManager trustManager;
    private final CustomX509KeyManager keyManager;

    @Autowired
    private CertificateConfig certConfig;

    public CertificateEventChangeDetector(AbstractEnvironment environment,
                                           KubernetesClient kubernetesClient,
                                           ConfigReloadProperties properties,
                                           CustomX509TrustManager trustManager,
                                           CustomX509KeyManager keyManager) {
        super(environment, kubernetesClient, properties);
        this.trustManager = trustManager;
        this.keyManager = keyManager;
    }

    @Override
    public void subscribe() {
        if (properties.isMonitoringSecrets()) {
            LOGGER.info("Certificate event detector is ENABLED");
            final var detectorName = this.toString();
            kubernetesClient.secrets().watch(new Subscriber<>(detectorName) {
                @Override
                public void eventReceived(final Action action, final Secret secret) {
                    onSecret(action, secret);
                }
            });
        }
    }

    private void onSecret(final Action action, final Secret secret) {
        var sourceName = secret.getMetadata().getName();
        if (null != certConfig) {
            certConfig.getSecrets().forEach(item -> {
                updateCertificates(item.getCertTruststoreName(), sourceName, item.getCertTruststoreField(), action, secret, true);
                updateCertificates(item.getCertKeystoreName(), sourceName, item.getCertKeystoreField(), action, secret, false);
            });
        }
    }

    void updateCertificates(String certName, String sourceName, String field, Action action, Secret secret, boolean isTrustStore){
        if (itemIsCertificateSourceWithCorrectAction(certName, sourceName, action)) {
            LOGGER.info("Detected change in secrets {} adding certificates", sourceName);
            var certs = Optional.ofNullable(secret.getData()).orElseGet(Maps::newHashMap).get(field);
            if (certs != null) {
                try {
                    if (isTrustStore) {
                        updateTrustManager(certs);
                    } else {
                        updateKeyManager(certs);
                    }
                    LOGGER.info("The action secret {} accepted with is {}", sourceName, action);
                } catch (CertificateException | KeyStoreException e) {
                    LOGGER.error("Failed to add certificates {} to {}:: {} ", certName, isTrustStore ? "trust manager" : "key manager", e.getMessage());
                }
            } else {
                LOGGER.warn("No certificate were found in secret {}", sourceName);
            }
        }
    }

    private boolean itemIsCertificateSourceWithCorrectAction(String itemName, String sourceName, Action action) {
        return sourceName.equals(itemName) && (ADDED.equals(action) || MODIFIED.equals(action));
    }

    void updateTrustManager(final String certs) throws CertificateException, KeyStoreException {
        byte[] certsBytes = Base64.getDecoder().decode(certs);
        var bytes = new ByteArrayInputStream(certsBytes);
        var cf = CertificateFactory.getInstance(X509_INSTANCE);
        Collection<? extends Certificate> x509certs = cf.generateCertificates(bytes);
        trustManager.addCertificates(x509certs);
    }

    void updateKeyManager(final String pkcs12) throws KeyStoreException, CertificateException {
        byte[] certsBytes = Base64.getDecoder().decode(pkcs12);
        InputStream is = new ByteArrayInputStream(certsBytes);
        keyManager.loadKeyManagerFactory(is);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

}
