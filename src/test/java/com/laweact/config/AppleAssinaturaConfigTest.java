package com.laweact.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.apple.itunes.storekit.verification.SignedDataVerifier;

@DisplayName("AppleAssinaturaConfig")
class AppleAssinaturaConfigTest {

    @Test
    @DisplayName("SignedDataVerifier sobe com as CAs raiz e bundle de staging/sandbox")
    void shouldBuildSandboxVerifierWithAppleRoots() {
        AssinaturaProperties properties = new AssinaturaProperties();
        properties.getApple().setBundleId("com.laweact.app.staging");
        properties.getApple().setEnvironment("SANDBOX");

        SignedDataVerifier verifier = new AppleAssinaturaConfig().signedDataVerifier(properties);

        assertThat(verifier).isNotNull();
    }
}
