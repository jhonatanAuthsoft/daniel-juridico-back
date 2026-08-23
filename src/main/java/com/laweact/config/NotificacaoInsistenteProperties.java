package com.laweact.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "laweact.notificacoes.insistente")
public class NotificacaoInsistenteProperties {

    /** Minimum time between insistent reminders for the same connection. */
    private Duration interval = Duration.ofHours(24);
}
