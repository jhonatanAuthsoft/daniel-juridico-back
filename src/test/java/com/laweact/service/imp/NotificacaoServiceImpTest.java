package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laweact.config.ExpoPushProperties;
import com.laweact.model.entity.DispositivoPushEntity;
import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.StatusEnvioNotificacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.repository.DispositivoPushRepository;
import com.laweact.repository.NotificacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ExpoPushClient;
import com.laweact.service.ExpoPushClient.ExpoPushSendResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService — criar e tentar enviar")
class NotificacaoServiceImpTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private DispositivoPushRepository dispositivoPushRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ExpoPushClient expoPushClient;

    private ExpoPushProperties expoPushProperties;
    private NotificacaoServiceImp service;

    private final UUID destinatarioId = UUID.randomUUID();
    private final UUID remetenteId = UUID.randomUUID();
    private final UUID conexaoId = UUID.randomUUID();

    private UsuarioEntity destinatario;
    private UsuarioEntity remetente;

    @BeforeEach
    void setUp() {
        expoPushProperties = new ExpoPushProperties();
        expoPushProperties.setEnabled(true);
        service = new NotificacaoServiceImp(
                notificacaoRepository,
                dispositivoPushRepository,
                usuarioRepository,
                expoPushClient,
                expoPushProperties
        );

        destinatario = UsuarioEntity.builder()
                .nomeCompleto("Advogado Destino")
                .email("adv@laweact.com")
                .senha("x")
                .notificacoesPushHabilitadas(true)
                .build();
        destinatario.setId(destinatarioId);

        remetente = UsuarioEntity.builder()
                .nomeCompleto("Cliente Remetente")
                .email("cli@laweact.com")
                .senha("x")
                .build();
        remetente.setId(remetenteId);

        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));
        when(notificacaoRepository.save(any(NotificacaoEntity.class))).thenAnswer(inv -> {
            NotificacaoEntity entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
    }

    @Test
    @DisplayName("preferência off → SKIPPED sem chamar Expo")
    void shouldSkipWhenPreferenceOff() {
        destinatario.setNotificacoesPushHabilitadas(false);

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.SKIPPED);
        verify(expoPushClient, never()).enviar(any(), any(), any(), any(), any(), any());
        verify(dispositivoPushRepository, never()).findByUsuario_IdAndAtivoTrue(any());
    }

    @Test
    @DisplayName("sem tokens ativos → SKIPPED")
    void shouldSkipWhenNoActiveTokens() {
        when(dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId)).thenReturn(List.of());

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.SKIPPED);
        verify(expoPushClient, never()).enviar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("push desabilitado → SKIPPED")
    void shouldSkipWhenPushDisabled() {
        expoPushProperties.setEnabled(false);

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.SKIPPED);
        verify(expoPushClient, never()).enviar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("envio ok → ENVIADA")
    void shouldMarkEnviadaOnSuccess() {
        DispositivoPushEntity device = dispositivo("ExponentPushToken[ok]");
        when(dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId))
                .thenReturn(List.of(device));
        when(expoPushClient.enviar(eq(device.getExpoPushToken()), any(), any(), any(), eq(conexaoId), any()))
                .thenReturn(new ExpoPushSendResult.Success());

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.ENVIADA);
        assertThat(result.getEnviadoEm()).isNotNull();
        assertThat(result.getErroEnvio()).isNull();
        verify(expoPushClient).enviar(
                eq(device.getExpoPushToken()),
                any(),
                any(),
                any(),
                eq(conexaoId),
                eq(null)
        );
    }

    @Test
    @DisplayName("erro Expo → ERROR com mensagem")
    void shouldMarkErrorOnExpoFailure() {
        DispositivoPushEntity device = dispositivo("ExponentPushToken[err]");
        when(dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId))
                .thenReturn(List.of(device));
        when(expoPushClient.enviar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ExpoPushSendResult.Error("timeout"));

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_ACEITA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.ERROR);
        assertThat(result.getErroEnvio()).contains("timeout");
        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("DeviceNotRegistered → desativa device e ERROR se nenhum sucesso")
    void shouldDeactivateDeviceNotRegistered() {
        DispositivoPushEntity device = dispositivo("ExponentPushToken[invalid]");
        when(dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId))
                .thenReturn(List.of(device));
        when(expoPushClient.enviar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ExpoPushSendResult.DeviceNotRegistered());
        when(dispositivoPushRepository.save(any(DispositivoPushEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificacaoEntity result = service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                null
        );

        assertThat(result.getStatusEnvio()).isEqualTo(StatusEnvioNotificacaoEnum.ERROR);
        ArgumentCaptor<DispositivoPushEntity> captor = ArgumentCaptor.forClass(DispositivoPushEntity.class);
        verify(dispositivoPushRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    @DisplayName("encaminha urgência emergência para o cliente Expo")
    void shouldForwardEmergencyUrgencyToExpo() {
        DispositivoPushEntity device = dispositivo("ExponentPushToken[ok]");
        when(dispositivoPushRepository.findByUsuario_IdAndAtivoTrue(destinatarioId))
                .thenReturn(List.of(device));
        when(expoPushClient.enviar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ExpoPushSendResult.Success());

        service.criarETentarEnviar(
                destinatarioId,
                remetenteId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                conexaoId,
                "Título",
                "Texto",
                UrgenciaSolicitacaoEnum.EMERGENCIA
        );

        verify(expoPushClient).enviar(
                eq(device.getExpoPushToken()),
                any(),
                any(),
                any(),
                eq(conexaoId),
                eq(UrgenciaSolicitacaoEnum.EMERGENCIA)
        );
    }

    private DispositivoPushEntity dispositivo(String token) {
        DispositivoPushEntity device = DispositivoPushEntity.builder()
                .usuario(destinatario)
                .expoPushToken(token)
                .ativo(true)
                .build();
        device.setId(UUID.randomUUID());
        return device;
    }
}
