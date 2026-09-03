package com.laweact.config;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Configuration
@ConditionalOnProperty(prefix = "laweact.assinatura.google", name = "enabled", havingValue = "true")
public class GoogleAssinaturaConfig {

    @Bean
    AndroidPublisher androidPublisher(AssinaturaProperties properties) throws Exception {
        AssinaturaProperties.Google google = properties.getGoogle();
        if (!StringUtils.hasText(google.getServiceAccountJson())) {
            throw new IllegalStateException(
                    "laweact.assinatura.google.service-account-json é obrigatório quando Google está habilitado"
            );
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(google.getServiceAccountJson().getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

        return new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("laweact-server")
                .build();
    }
}
