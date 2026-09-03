package com.laweact.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "laweact.assinatura")
public class AssinaturaProperties {

    private Duration trialDuration = Duration.ofDays(30);
    private Duration graceDuration = Duration.ofDays(3);
    private String productId = "laweact_basic_mensal";
    private FakeStore fakeStore = new FakeStore();
    private Apple apple = new Apple();
    private Google google = new Google();

    @Data
    public static class FakeStore {
        private boolean enabled = false;
        private Duration activeDuration = Duration.ofMinutes(5);
    }

    @Data
    public static class Apple {
        private boolean enabled = false;
        private String bundleId = "com.laweact.app";
        private String environment = "SANDBOX";
        private String issuerId;
        private String keyId;
        private String privateKey;
    }

    @Data
    public static class Google {
        private boolean enabled = false;
        private String packageName = "com.laweact.app";
        private String serviceAccountJson;
    }
}
