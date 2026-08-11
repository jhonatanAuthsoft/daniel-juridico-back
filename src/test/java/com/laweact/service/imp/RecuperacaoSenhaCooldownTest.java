package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RecuperacaoSenha — cooldown progressivo")
class RecuperacaoSenhaCooldownTest {

    @Test
    @DisplayName("primeiro envio sem espera; 2º e 3º com 5s; depois +1 min")
    void shouldFollowProgressiveCooldown() {
        assertThat(RecuperacaoSenhaServiceImp.calcularCooldownSegundos(0)).isZero();
        assertThat(RecuperacaoSenhaServiceImp.calcularCooldownSegundos(1)).isEqualTo(5);
        assertThat(RecuperacaoSenhaServiceImp.calcularCooldownSegundos(2)).isEqualTo(5);
        assertThat(RecuperacaoSenhaServiceImp.calcularCooldownSegundos(3)).isEqualTo(60);
        assertThat(RecuperacaoSenhaServiceImp.calcularCooldownSegundos(4)).isEqualTo(120);
    }
}
