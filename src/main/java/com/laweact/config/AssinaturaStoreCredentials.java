package com.laweact.config;

import org.springframework.util.StringUtils;

public final class AssinaturaStoreCredentials {

    private AssinaturaStoreCredentials() {
    }

    public static boolean isAppleReady(AssinaturaProperties.Apple apple) {
        return apple != null
                && apple.isEnabled()
                && StringUtils.hasText(apple.getBundleId())
                && StringUtils.hasText(apple.getIssuerId())
                && StringUtils.hasText(apple.getKeyId())
                && StringUtils.hasText(apple.getPrivateKey());
    }

    public static boolean isGoogleReady(AssinaturaProperties.Google google) {
        return google != null
                && google.isEnabled()
                && StringUtils.hasText(google.getPackageName())
                && StringUtils.hasText(google.getServiceAccountJson());
    }

    public static String normalizeMultiline(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\n", "\n").trim();
    }
}
