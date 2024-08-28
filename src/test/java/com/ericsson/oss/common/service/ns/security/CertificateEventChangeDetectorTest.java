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

import com.ericsson.oss.common.service.ns.NotificationServiceApplication;
import com.ericsson.oss.common.service.ns.configurations.CertificateConfig;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.logging.log4j.util.Strings;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.kubernetes.commons.config.reload.ConfigReloadProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ericsson.oss.common.service.ns.security.CustomX509TrustManager.TEMP_KEYSTORE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = NotificationServiceApplication.class)
@ActiveProfiles("dev")
@DirtiesContext
@TestPropertySource(properties = { "spring.cloud.kubernetes.enabled = false", "truststore.path = ${java.home}/lib/security/cacerts", "spring.kafka.enabled = false" })
@ExtendWith(SpringExtension.class)
class CertificateEventChangeDetectorTest {

    public static final String KEYSTORE_PASS = "changeit";

    @Autowired
    CustomX509TrustManager customX509TrustManager;

    @Autowired
    CustomX509KeyManager customX509KeyManager;

    @Mock
    AbstractEnvironment environment;

    @Mock
    ConfigReloadProperties properties;

    @Mock
    KubernetesClient kubernetesClient;

    @Rule
    public KubernetesServer server = new KubernetesServer(false);

    @Spy
    @InjectMocks
    CertificateEventChangeDetector detector;

    @Test
    void onEvent() throws IOException, URISyntaxException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        CertificateEventChangeDetector certificateEventChangeDetector = new CertificateEventChangeDetector(environment, kubernetesClient,
                properties, customX509TrustManager, customX509KeyManager);

        String certs = getResourceContent("testCert.crt");
        ReflectionTestUtils.setField(certificateEventChangeDetector, "trustManager", customX509TrustManager);
        certificateEventChangeDetector.updateTrustManager(certs);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(System.getProperty("java.io.tmpdir") + File.separator + TEMP_KEYSTORE), KEYSTORE_PASS.toCharArray());

        byte[] certsBytes = Base64.getDecoder().decode(certs);
        ByteArrayInputStream bytes = new ByteArrayInputStream(certsBytes);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> x509certs = cf.generateCertificates(bytes);

        X509Certificate testCert = (X509Certificate) x509certs.iterator().next();
        String alias = ks.getCertificateAlias(testCert);
        assertFalse(Strings.isEmpty(alias), "Did not find cert in trustStore");
    }

    @Test
    void checkCertificatesAdded() throws KeyStoreException, CertificateException {
        Map<String, String> secretData = new HashMap<>();
        secretData.put("tls.crt", "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURiVENDQWxXZ0F3SUJBZ0lVY25PYllqZzFGOHBRNW1RN2lpa05SaDMreU1rd0RRWUpLb1pJaHZjTkFRRUwKQlFBd1JURUxNQWtHQTFVRUJoTUNRVlV4RXpBUkJnTlZCQWdNQ2xOdmJXVXRVM1JoZEdVeElUQWZCZ05WQkFvTQpHRWx1ZEdWeWJtVjBJRmRwWkdkcGRITWdVSFI1SUV4MFpEQWdGdzB5TWpBMU1UQXhNVEF5TXpOYUdBOHlNamsyCk1ESXlNekV4TURJek0xb3dSVEVMTUFrR0ExVUVCaE1DUVZVeEV6QVJCZ05WQkFnTUNsTnZiV1V0VTNSaGRHVXgKSVRBZkJnTlZCQW9NR0VsdWRHVnlibVYwSUZkcFpHZHBkSE1nVUhSNUlFeDBaRENDQVNJd0RRWUpLb1pJaHZjTgpBUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBS1gyU2d6VFpCVnpENjVSYWRHUUc5cGdIS01uUGhldjJ6OXFhMFBTClRKdTJZTnFTckt5RFl2K0hRdnU5MjBNT0NPQk9JanhzWkJWUlVZUDczZEt5TkhVR0piaTBndldBUDRZYkM3SFoKa0kxcUVveXdtZllobS9waG1zZkxrOUNXdjJ6U1YyZDh3TWUvUW9MWDVUMjdza0dvekNzZ0xBUUtkZmhMYmNyQwp3eG9neFVoSFU4OTlZa1pnQjVSRXNkSDkzMzBNaHpCQlJxdXc2a1JKdWhaUVYrS0VPOVZRYWZ0dkR6VElPTkE4CjZCa1hwZ1Z4R1d4N0xQRDhvby9iK3FCbEpSNDZpMENZMVFjdG9QeURnWGxhS0lNb29qNGN0VGcyWFFhV08zL0kKcytPU0pOdXl2NTBpeFVOQTJTczU4UzhST1lGT0RpTGNVbXF1b1VhOUQvRU14dWtDQXdFQUFhTlRNRkV3SFFZRApWUjBPQkJZRUZObGdHallSOUliTFR0b1M1a3RaQkIvL0QwcnJNQjhHQTFVZEl3UVlNQmFBRk5sZ0dqWVI5SWJMClR0b1M1a3RaQkIvL0QwcnJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUIKQUhZNlRxTi8zakpNT0FFUW5SN1M4REZXSWNsQWxNZ3Fmcm9uZ1lLcGh1bElrMFVWSWxkak9qS2l2cG96SGZhUApQRlY0cjlyOVkrL0hBMzh3bGYxTmIzR3ZRKzR5amhMS1FQdUdVS2dKdFNySmltMUkxaHZJYWZwOEduVVdkSVNjCno2Szd0dXV1S1paTmJ3LzNFU2pGbmg0Q3BGWnNQQUtWb3JqR2kvaEVKSWtJWG9tZkxBT0pTY0VONEZGc09EMkcKcUpSNWh5ZFNPRFE1eXJTUkNHejZaWk0yT0lITkhhUVQzQWlPK1MrUlNBMmROaG9jcGw3VC9ZSEVhZ0hCVjJmTgowb0d6RUY5aC90eERNYjEzVjdFWmsrNDFyaS9KcTZpV2wyTCtybTlJY2V0anJlWHlEUkRvSCtrNFNqWGhZbkh1Ckd1eEJqNElhTmZLcmlXNkZQWHZpNzdnPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0t");
        Map<String, String> clientSecretData = new HashMap<>();
        clientSecretData.put("client.p12", "MIINWQIBAzCCDR8GCSqGSIb3DQEHAaCCDRAEgg0MMIINCDCCB78GCSqGSIb3DQEHBqCCB7AwggesAgMIINWQIBAzCCDR8GCSqGSIb3DQEHAaCCDRAEgg0MMIINCDCCB78GCSqGSIb3DQEHBqCCB7AwggesAgGdzB5TWpBMU1UQXhNVEF5TXpOYUdBOHlNamsyCk1ESXlNekV4TURJek0xb3dSVEVMTUFrR0ExVUVCaE1DUVZVeEV6QVJCZ05WQkFnTUNsTnZiV1V0VTNSaGRHVXgKSVRBZkJnTlZCQW9NR0VsdWRHVnlibVYwSUZkcFpHZHBkSE1nVUhSNUlFeDBaRENDQVNJd0RRWUpLb1pJaHZjTgpBUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBS1gyU2d6VFpCVnpENjVSYWRHUUc5cGdIS01uUGhldjJ6OXFhMFBTClRKdTJZTnFTckt5RFl2K0hRdnU5MjBNT0NPQk9JanhzWkJWUlVZUDczZEt5TkhVR0piaTBndldBUDRZYkM3SFoKa0kxcUVveXdtZllobS9waG1zZkxrOUNXdjJ6U1YyZDh3TWUvUW9MWDVUMjdza0dvekNzZ0xBUUtkZmhMYmNyQwp3eG9neFVoSFU4OTlZa1pnQjVSRXNkSDkzMzBNaHpCQlJxdXc2a1JKdWhaUVYrS0VPOVZRYWZ0dkR6VElPTkE4CjZCa1hwZ1Z4R1d4N0xQRDhvby9iK3FCbEpSNDZpMENZMVFjdG9QeURnWGxhS0lNb29qNGN0VGcyWFFhV08zL0kKcytPU0pOdXl2NTBpeFVOQTJTczU4UzhST1lGT0RpTGNVbXF1b1VhOUQvRU14dWtDQXdFQUFhTlRNRkV3SFFZRApWUjBPQkJZRUZObGdHallSOUliTFR0b1M1a3RaQkIvL0QwcnJNQjhHQTFVZEl3UVlNQmFBRk5sZ0dqWVI5SWJMClR0b1M1a3RaQkIvL0QwcnJNQThHQTFVZEV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUIKQUhZNlRxTi8zakpNT0FFUW5SN1M4REZXSWNsQWxNZ3Fmcm9uZ1lLcGh1bElrMFVWSWxkak9qS2l2cG96SGZhUApQRlY0cjlyOVkrL0hBMzh3bGYxTmIzR3ZRKzR5amhMS1FQdUdVS2dKdFNySmltMUkxaHZJYWZwOEduVVdkSVNjCno2Szd0dXV1S1paTmJ3LzNFU2pGbmg0Q3BGWnNQQUtWb3JqR2kvaEVKSWtJWG9tZkxBT0pTY0VONEZGc09EMkcKcUpSNWh5ZFNPRFE1eXJTUkNHejZaWk0yT0lITkhhUVQzQWlPK1MrUlNBMmROaG9jcGw3VC9ZSEVhZ0hCVjJmTgowb0d6RUY5aC90eERNYjEzVjdFWmsrNDFyaS9KcTZpV2wyTCtybTlJY2V0anJlWHlEUk");

        Secret secret = new SecretBuilder().addToData(secretData).withNewMetadata().withName("iam-cacert-secret").withNamespace("test").endMetadata().build();
        Secret clientSecret = new SecretBuilder().addToData(clientSecretData).withNewMetadata().withName("eric-oss-notifications-client-secret").withNamespace("test").endMetadata().build();

        ConfigReloadProperties properties = new ConfigReloadProperties();
        properties.setMonitoringSecrets(true);

        CustomX509TrustManager trustManager  = mock(CustomX509TrustManager.class);
        CustomX509KeyManager keyManager  = mock(CustomX509KeyManager.class);

        CertificateEventChangeDetector changeDetector = new CertificateEventChangeDetector(null, server.getClient(), properties, trustManager, keyManager);

        CertificateConfig certificateConfig = new CertificateConfig();
        CertificateConfig.Secrets secretsSources = new CertificateConfig.Secrets();
        secretsSources.setCertTruststoreName("iam-cacert-secret");
        secretsSources.setCertTruststoreField("tls.crt");
        secretsSources.setCertKeystoreName("eric-oss-notifications-client-secret");
        secretsSources.setCertKeystoreField("client.p12");
        certificateConfig.setSecrets(List.of(secretsSources));

        ReflectionTestUtils.setField(changeDetector, "certConfig", certificateConfig);

        changeDetector.updateCertificates(certificateConfig.getSecrets().get(0).getCertTruststoreName(), secret.getMetadata().getName(), certificateConfig.getSecrets().get(0).getCertTruststoreField(),Watcher.Action.ADDED, secret, true);
        verify(trustManager, times(1)).addCertificates(any());

        changeDetector.updateCertificates(certificateConfig.getSecrets().get(0).getCertKeystoreName(), clientSecret.getMetadata().getName(), certificateConfig.getSecrets().get(0).getCertKeystoreField(),Watcher.Action.ADDED, clientSecret, false);
        verify(keyManager, times(1)).loadKeyManagerFactory(any());

    }

    @Test
    void testReSubscription() throws NoSuchFieldException {

        //given
        doNothing().when(detector).subscribe();
        ReflectionTestUtils.setField(detector, "initialTimeout", 0L);

        //when WatchConnectionManager closes it calls this method through delegate e.g. Subscriber Impl
        detector.scheduleResubscribe();

        //then
        verify(detector, timeout(TimeUnit.SECONDS.toMillis(1)).atLeastOnce()).subscribe();
    }

    public static String getResourceContent(String fileName) throws URISyntaxException, IOException {
        return new String(Files.readAllBytes(Paths.get(Resources.getResource(fileName).toURI())));
    }
}
