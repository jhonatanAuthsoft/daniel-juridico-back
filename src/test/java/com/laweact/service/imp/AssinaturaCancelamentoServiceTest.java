package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.apple.itunes.storekit.model.AutoRenewStatus;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.repository.AssinaturaEventoRepository;
import com.laweact.repository.AssinaturaRepository;
import com.laweact.service.AssinaturaAcessoService;
import com.laweact.config.AssinaturaProperties;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Assinatura — cancelamento durante o mês grátis (Apple)")
class AssinaturaCancelamentoServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private AssinaturaEventoRepository assinaturaEventoRepository;

    private final AssinaturaAcessoService acessoService =
            new AssinaturaAcessoService(new AssinaturaProperties());

    private AssinaturaNotificationServiceImp service() {
        return new AssinaturaNotificationServiceImp(
                assinaturaRepository,
                assinaturaEventoRepository,
                null,
                List.of(),
                new ObjectMapper()
        );
    }

    private AssinaturaEntity assinaturaAtiva(LocalDateTime periodoFimEm) {
        UsuarioEntity advogado = new UsuarioEntity();
        advogado.setPerfil(PerfilUsuarioEnum.ADVOGADO);
        return AssinaturaEntity.builder()
                .usuario(advogado)
                .status(StatusAssinaturaEnum.ATIVA)
                .periodoFimEm(periodoFimEm)
                .autoRenovacao(true)
                .build();
    }

    private JWSTransactionDecodedPayload transacaoExpirandoEm(LocalDateTime expiracao) {
        return new JWSTransactionDecodedPayload()
                .expiresDate(expiracao.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("cancelou no mês grátis: marca CANCELADA, zera autorenovação e mantém o acesso")
    void shouldKeepAccessWhenRenewalIsTurnedOff() {
        LocalDateTime fimDoMesGratis = LocalDateTime.now().plusDays(23);
        AssinaturaEntity assinatura = assinaturaAtiva(fimDoMesGratis);

        service().atualizarPorTipoApple(
                assinatura,
                "DID_CHANGE_RENEWAL_STATUS",
                transacaoExpirandoEm(fimDoMesGratis),
                AutoRenewStatus.OFF
        );

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.CANCELADA);
        assertThat(assinatura.getAutoRenovacao()).isFalse();
        assertThat(acessoService.isAcessoLiberado(assinatura.getUsuario(), assinatura)).isTrue();
    }

    @Test
    @DisplayName("reativou a renovação: volta para ATIVA")
    void shouldReactivateWhenRenewalIsTurnedBackOn() {
        LocalDateTime fim = LocalDateTime.now().plusDays(10);
        AssinaturaEntity assinatura = assinaturaAtiva(fim);
        assinatura.setStatus(StatusAssinaturaEnum.CANCELADA);
        assinatura.setAutoRenovacao(false);

        service().atualizarPorTipoApple(
                assinatura,
                "DID_CHANGE_RENEWAL_STATUS",
                transacaoExpirandoEm(fim),
                AutoRenewStatus.ON
        );

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.ATIVA);
        assertThat(assinatura.getAutoRenovacao()).isTrue();
    }

    @Test
    @DisplayName("fim do mês grátis sem renovar: EXPIRADA e acesso bloqueado")
    void shouldBlockWhenFreeTrialExpires() {
        LocalDateTime fim = LocalDateTime.now().minusMinutes(1);
        AssinaturaEntity assinatura = assinaturaAtiva(fim);
        assinatura.setStatus(StatusAssinaturaEnum.CANCELADA);

        service().atualizarPorTipoApple(
                assinatura,
                "EXPIRED",
                transacaoExpirandoEm(fim),
                AutoRenewStatus.OFF
        );

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.EXPIRADA);
        assertThat(acessoService.isAcessoLiberado(assinatura.getUsuario(), assinatura)).isFalse();
    }

    @Test
    @DisplayName("renovou no fim do mês grátis: ATIVA com novo período")
    void shouldRenewAfterFreeTrial() {
        LocalDateTime novoFim = LocalDateTime.now().plusDays(30);
        AssinaturaEntity assinatura = assinaturaAtiva(LocalDateTime.now());

        service().atualizarPorTipoApple(
                assinatura,
                "DID_RENEW",
                transacaoExpirandoEm(novoFim),
                AutoRenewStatus.ON
        );

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.ATIVA);
        assertThat(assinatura.getPeriodoFimEm())
                .isCloseTo(novoFim, org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("grace period vencido: EXPIRADA")
    void shouldExpireAfterGracePeriod() {
        AssinaturaEntity assinatura = assinaturaAtiva(LocalDateTime.now().minusDays(1));
        assinatura.setStatus(StatusAssinaturaEnum.EM_ATRASO);

        service().atualizarPorTipoApple(
                assinatura,
                "GRACE_PERIOD_EXPIRED",
                transacaoExpirandoEm(LocalDateTime.now().minusDays(1)),
                AutoRenewStatus.OFF
        );

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.EXPIRADA);
    }

    @Test
    @DisplayName("sem renewal info: não mexe na autorenovação")
    void shouldKeepAutoRenewWhenRenewalInfoIsAbsent() {
        AssinaturaEntity assinatura = assinaturaAtiva(LocalDateTime.now().plusDays(5));

        service().atualizarPorTipoApple(
                assinatura,
                "DID_CHANGE_RENEWAL_STATUS",
                transacaoExpirandoEm(LocalDateTime.now().plusDays(5)),
                null
        );

        assertThat(assinatura.getAutoRenovacao()).isTrue();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinaturaEnum.ATIVA);
        assertThat(assinatura.getUltimaSincronizacaoEm())
                .isAfter(Instant.now().minusSeconds(5).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
