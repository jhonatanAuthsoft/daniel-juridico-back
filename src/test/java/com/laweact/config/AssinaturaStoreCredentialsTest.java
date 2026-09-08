package com.laweact.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssinaturaStoreCredentials — homolog sandbox")
class AssinaturaStoreCredentialsTest {

    @Test
    @DisplayName("Apple só fica pronta com enabled + issuer + keyId + PEM")
    void shouldRequireAppleSecrets() {
        AssinaturaProperties.Apple apple = new AssinaturaProperties.Apple();
        apple.setEnabled(true);
        apple.setBundleId("com.laweact.app.staging");

        assertThat(AssinaturaStoreCredentials.isAppleReady(apple)).isFalse();

        apple.setIssuerId("issuer");
        apple.setKeyId("key");
        apple.setPrivateKey("-----BEGIN PRIVATE KEY-----\\nABC\\n-----END PRIVATE KEY-----");

        assertThat(AssinaturaStoreCredentials.isAppleReady(apple)).isTrue();
    }

    @Test
    @DisplayName("Google só fica pronta com enabled + JSON da service account")
    void shouldRequireGoogleSecrets() {
        AssinaturaProperties.Google google = new AssinaturaProperties.Google();
        google.setEnabled(true);
        google.setPackageName("com.laweact.app.staging");

        assertThat(AssinaturaStoreCredentials.isGoogleReady(google)).isFalse();

        google.setServiceAccountJson("{\"type\":\"service_account\"}");

        assertThat(AssinaturaStoreCredentials.isGoogleReady(google)).isTrue();
    }

    @Test
    @DisplayName("PEM vinda de env com \\\\n vira quebra de linha real")
    void shouldNormalizeEscapedPemNewlines() {
        String normalized = AssinaturaStoreCredentials.normalizeMultiline(
                "-----BEGIN PRIVATE KEY-----\\nABC\\n-----END PRIVATE KEY-----"
        );

        assertThat(normalized).isEqualTo("-----BEGIN PRIVATE KEY-----\nABC\n-----END PRIVATE KEY-----");
    }
}
