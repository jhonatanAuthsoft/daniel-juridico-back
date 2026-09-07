package com.laweact.service;

import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

/**
 * Maps solicitation urgency to Expo Push delivery hints.
 * Android uses {@code channelId}; iOS uses {@code interruptionLevel}.
 */
public final class ExpoPushPriority {

    public static final String CHANNEL_DEFAULT = "default";
    public static final String CHANNEL_URGENT = "urgent";

    private ExpoPushPriority() {}

    public static boolean isHigh(UrgenciaSolicitacaoEnum urgencia) {
        return urgencia == UrgenciaSolicitacaoEnum.EMERGENCIA
                || urgencia == UrgenciaSolicitacaoEnum.URGENTE;
    }

    public static String priority(UrgenciaSolicitacaoEnum urgencia) {
        return isHigh(urgencia) ? "high" : "default";
    }

    public static String channelId(UrgenciaSolicitacaoEnum urgencia) {
        return isHigh(urgencia) ? CHANNEL_URGENT : CHANNEL_DEFAULT;
    }

    public static String interruptionLevel(UrgenciaSolicitacaoEnum urgencia) {
        return isHigh(urgencia) ? "time-sensitive" : "active";
    }
}
