package com.laweact.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laweact.config.AssinaturaProperties;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

@DisplayName("AssinaturaAcessoService — mês grátis da loja")
class AssinaturaAcessoServiceTest {

    private final AssinaturaProperties properties = new AssinaturaProperties();
    private final AssinaturaAcessoService service = new AssinaturaAcessoService(properties);

    private UsuarioEntity advogado() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setPerfil(PerfilUsuarioEnum.ADVOGADO);
        return usuario;
    }

    private AssinaturaEntity assinatura(StatusAssinaturaEnum status, LocalDateTime periodoFimEm) {
        return AssinaturaEntity.builder()
                .status(status)
                .periodoFimEm(periodoFimEm)
                .build();
    }

    @Test
    @DisplayName("cliente sempre tem acesso")
    void shouldAllowClient() {
        UsuarioEntity cliente = new UsuarioEntity();
        cliente.setPerfil(PerfilUsuarioEnum.CLIENTE);

        assertThat(service.isAcessoLiberado(cliente, null)).isTrue();
    }

    @Test
    @DisplayName("advogado sem assinatura fica bloqueado")
    void shouldBlockLawyerWithoutSubscription() {
        assertThat(service.isAcessoLiberado(advogado(), null)).isFalse();
    }

    @Test
    @DisplayName("advogado recém-cadastrado (PENDENTE) fica bloqueado no paywall")
    void shouldBlockPendingSubscription() {
        AssinaturaEntity pendente = assinatura(StatusAssinaturaEnum.PENDENTE, null);

        assertThat(service.isAcessoLiberado(advogado(), pendente)).isFalse();
    }

    @Test
    @DisplayName("assinatura ativa libera até o fim do período")
    void shouldAllowActiveSubscription() {
        AssinaturaEntity ativa = assinatura(StatusAssinaturaEnum.ATIVA, LocalDateTime.now().plusDays(10));

        assertThat(service.isAcessoLiberado(advogado(), ativa)).isTrue();
    }

    @Test
    @DisplayName("cancelou durante o mês grátis: mantém acesso até o fim do período")
    void shouldKeepAccessAfterCancellingDuringFreeTrial() {
        AssinaturaEntity cancelada = assinatura(StatusAssinaturaEnum.CANCELADA, LocalDateTime.now().plusDays(20));

        assertThat(service.isAcessoLiberado(advogado(), cancelada)).isTrue();
    }

    @Test
    @DisplayName("cancelada e com período vencido bloqueia")
    void shouldBlockCancelledAfterPeriodEnd() {
        AssinaturaEntity cancelada = assinatura(StatusAssinaturaEnum.CANCELADA, LocalDateTime.now().minusMinutes(1));

        assertThat(service.isAcessoLiberado(advogado(), cancelada)).isFalse();
    }

    @Test
    @DisplayName("ativa com período vencido bloqueia")
    void shouldBlockActiveAfterPeriodEnd() {
        AssinaturaEntity ativa = assinatura(StatusAssinaturaEnum.ATIVA, LocalDateTime.now().minusMinutes(1));

        assertThat(service.isAcessoLiberado(advogado(), ativa)).isFalse();
    }

    @Test
    @DisplayName("em atraso libera dentro do grace period")
    void shouldAllowBillingRetryInsideGrace() {
        AssinaturaEntity emAtraso = assinatura(StatusAssinaturaEnum.EM_ATRASO, LocalDateTime.now().minusDays(1));

        assertThat(service.isAcessoLiberado(advogado(), emAtraso)).isTrue();
    }

    @Test
    @DisplayName("em atraso bloqueia depois do grace period")
    void shouldBlockBillingRetryAfterGrace() {
        AssinaturaEntity emAtraso = assinatura(StatusAssinaturaEnum.EM_ATRASO, LocalDateTime.now().minusDays(5));

        assertThat(service.isAcessoLiberado(advogado(), emAtraso)).isFalse();
    }

    @Test
    @DisplayName("expirada bloqueia")
    void shouldBlockExpired() {
        AssinaturaEntity expirada = assinatura(StatusAssinaturaEnum.EXPIRADA, LocalDateTime.now().plusDays(10));

        assertThat(service.isAcessoLiberado(advogado(), expirada)).isFalse();
    }
}
