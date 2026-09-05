package com.laweact.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "laweact.notificacoes.insistente")
public class NotificacaoInsistenteProperties {

    /** Minimum time between insistent reminders for the same connection. */
    private Duration interval = Duration.ofHours(12);

    /** Cron of the in-process scheduler (every hour by default). Eligibility still uses `interval`. */
    private String cron = "0 0 * * * *";

    /** When false, the Spring scheduler is not registered (HTTP job still works). */
    private boolean schedulerEnabled = true;
}
