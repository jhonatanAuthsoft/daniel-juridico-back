package com.laweact.config;

import java.io.InputStream;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.verification.SignedDataVerifier;

@Configuration
@Conditional(AppleAssinaturaEnabledCondition.class)
public class AppleAssinaturaConfig {

    @Bean
    AppStoreServerAPIClient appStoreServerAPIClient(AssinaturaProperties properties) {
        AssinaturaProperties.Apple apple = properties.getApple();
        Environment environment = resolveEnvironment(apple.getEnvironment());
        return new AppStoreServerAPIClient(
                AssinaturaStoreCredentials.normalizeMultiline(apple.getPrivateKey()),
                apple.getKeyId(),
                apple.getIssuerId(),
                apple.getBundleId(),
                environment
        );
    }

    @Bean
    SignedDataVerifier signedDataVerifier(AssinaturaProperties properties) {
        AssinaturaProperties.Apple apple = properties.getApple();
        Environment environment = resolveEnvironment(apple.getEnvironment());
        Set<InputStream> rootCerts = AppleRootCertificates.open();
        try {
            return new SignedDataVerifier(
                    rootCerts,
                    apple.getBundleId(),
                    apple.getAppAppleId(),
                    environment,
                    true
            );
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao configurar SignedDataVerifier da Apple", e);
        } finally {
            AppleRootCertificates.closeQuietly(rootCerts);
        }
    }

    private static Environment resolveEnvironment(String value) {
        return "PRODUCTION".equalsIgnoreCase(value) ? Environment.PRODUCTION : Environment.SANDBOX;
    }
}
