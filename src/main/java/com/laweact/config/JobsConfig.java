package com.laweact.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        JobsApiKeyProperties.class,
        NotificacaoInsistenteProperties.class,
        AssinaturaProperties.class
})
public class JobsConfig {
}
