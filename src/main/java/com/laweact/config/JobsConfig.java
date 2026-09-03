package com.laweact.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JobsApiKeyProperties.class,
        NotificacaoInsistenteProperties.class,
        AssinaturaProperties.class
})
public class JobsConfig {
}
