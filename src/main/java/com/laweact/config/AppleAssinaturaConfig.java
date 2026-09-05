package com.laweact.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.verification.SignedDataVerifier;

@Configuration
@ConditionalOnProperty(prefix = "laweact.assinatura.apple", name = "enabled", havingValue = "true")
public class AppleAssinaturaConfig {

    @Bean
    AppStoreServerAPIClient appStoreServerAPIClient(AssinaturaProperties properties) {
        AssinaturaProperties.Apple apple = properties.getApple();
        if (!StringUtils.hasText(apple.getPrivateKey())) {
            throw new IllegalStateException("laweact.assinatura.apple.private-key é obrigatório quando Apple está habilitado");
        }
        Environment environment = "PRODUCTION".equalsIgnoreCase(apple.getEnvironment())
                ? Environment.PRODUCTION
                : Environment.SANDBOX;
        return new AppStoreServerAPIClient(
                apple.getPrivateKey(),
                apple.getKeyId(),
                apple.getIssuerId(),
                apple.getBundleId(),
                environment
        );
    }

    @Bean
    SignedDataVerifier signedDataVerifier(AssinaturaProperties properties) {
        AssinaturaProperties.Apple apple = properties.getApple();
        Environment environment = "PRODUCTION".equalsIgnoreCase(apple.getEnvironment())
                ? Environment.PRODUCTION
                : Environment.SANDBOX;
        try (InputStream rootCert = loadAppleRootCert()) {
            return new SignedDataVerifier(
                    Set.of(rootCert),
                    apple.getBundleId(),
                    null,
                    environment,
                    true
            );
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao configurar SignedDataVerifier da Apple", e);
        }
    }

    private InputStream loadAppleRootCert() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
