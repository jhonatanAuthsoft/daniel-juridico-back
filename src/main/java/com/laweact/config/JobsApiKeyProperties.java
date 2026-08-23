package com.laweact.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "laweact.jobs")
public class JobsApiKeyProperties {

    /** Shared secret for infra cron callers (`X-Api-Key`). Empty = reject all. */
    private String apiKey = "";

    public boolean matches(String provided) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(provided)) {
            return false;
        }
        return apiKey.equals(provided);
    }
}
