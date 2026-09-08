package com.laweact.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppleRootCertificates")
class AppleRootCertificatesTest {

    @Test
    @DisplayName("carrega as CAs raiz da Apple em DER para verificar JWS do sandbox")
    void shouldLoadAppleRootCas() throws Exception {
        Set<InputStream> streams = AppleRootCertificates.open();
        try {
            assertThat(streams).hasSizeGreaterThanOrEqualTo(2);

            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (InputStream stream : streams) {
                assertThat(factory.generateCertificate(stream)).isInstanceOf(X509Certificate.class);
            }
        } finally {
            AppleRootCertificates.closeQuietly(streams);
        }
    }
}
