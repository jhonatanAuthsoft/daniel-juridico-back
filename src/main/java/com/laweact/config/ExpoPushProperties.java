package com.laweact.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "laweact.push")
public class ExpoPushProperties {

    /** Quando false, o envio Expo é ignorado (útil em testes). */
    private boolean enabled = true;

    private String baseUrl = "https://exp.host/--/api/v2/push";
}
