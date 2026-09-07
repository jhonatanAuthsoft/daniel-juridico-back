package com.laweact.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("ExpoPushPriority")
class ExpoPushPriorityTest {

    @Test
    @DisplayName("emergência e urgente usam prioridade alta")
    void shouldUseHighPriorityForEmergencyAndUrgent() {
        assertThat(ExpoPushPriority.isHigh(UrgenciaSolicitacaoEnum.EMERGENCIA)).isTrue();
        assertThat(ExpoPushPriority.isHigh(UrgenciaSolicitacaoEnum.URGENTE)).isTrue();
        assertThat(ExpoPushPriority.priority(UrgenciaSolicitacaoEnum.EMERGENCIA)).isEqualTo("high");
        assertThat(ExpoPushPriority.channelId(UrgenciaSolicitacaoEnum.URGENTE))
                .isEqualTo(ExpoPushPriority.CHANNEL_URGENT);
        assertThat(ExpoPushPriority.interruptionLevel(UrgenciaSolicitacaoEnum.EMERGENCIA))
                .isEqualTo("time-sensitive");
    }

    @Test
    @DisplayName("médio, tenho tempo e nulo usam prioridade secundária")
    void shouldUseSecondaryPriorityOtherwise() {
        assertThat(ExpoPushPriority.isHigh(UrgenciaSolicitacaoEnum.MEDIO)).isFalse();
        assertThat(ExpoPushPriority.isHigh(UrgenciaSolicitacaoEnum.TENHO_TEMPO)).isFalse();
        assertThat(ExpoPushPriority.isHigh(null)).isFalse();
        assertThat(ExpoPushPriority.priority(UrgenciaSolicitacaoEnum.MEDIO)).isEqualTo("default");
        assertThat(ExpoPushPriority.channelId(null)).isEqualTo(ExpoPushPriority.CHANNEL_DEFAULT);
        assertThat(ExpoPushPriority.interruptionLevel(UrgenciaSolicitacaoEnum.TENHO_TEMPO))
                .isEqualTo("active");
    }
}
