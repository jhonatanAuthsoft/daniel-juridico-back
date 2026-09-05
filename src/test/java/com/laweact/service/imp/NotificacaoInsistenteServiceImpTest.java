package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laweact.config.NotificacaoInsistenteProperties;
import com.laweact.dto.job.NotificacaoInsistenteJobResultDTO;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.NotificacaoRepository;
import com.laweact.service.NotificacaoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoInsistenteService")
class NotificacaoInsistenteServiceImpTest {

    @Mock
    private ConexaoRepository conexaoRepository;

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private NotificacaoService notificacaoService;

    private NotificacaoInsistenteProperties properties;
    private NotificacaoInsistenteServiceImp service;

    @BeforeEach
    void setUp() {
        properties = new NotificacaoInsistenteProperties();
        service = new NotificacaoInsistenteServiceImp(
                conexaoRepository,
                notificacaoRepository,
                notificacaoService,
                properties
        );
    }

    @Test
    @DisplayName("reusa notificação existente e atualiza ultimo_lembrete")
    void shouldReuseExistingNotification() {
        UUID conexaoId = UUID.randomUUID();
        ConexaoEntity conexao = conexaoPendente(conexaoId);

        NotificacaoEntity existente = NotificacaoEntity.builder()
                .titulo("Nova solicitação de conexão")
                .texto("old")
                .tipo(TipoNotificacaoEnum.CONEXAO_SOLICITADA)
                .build();
        existente.setId(UUID.randomUUID());

        when(conexaoRepository.findPendentesParaLembreteInsistente(
                eq(StatusConexaoEnum.PENDENTE),
                any(Set.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(conexao));
        when(notificacaoRepository.findFirstByReferenciaIdAndTipoOrderByCreatedAtAsc(
                conexaoId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA
        )).thenReturn(Optional.of(existente));
        when(notificacaoService.reinsistirEnvio(any(), any(), any())).thenReturn(existente);
        when(conexaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificacaoInsistenteJobResultDTO result = service.processar();

        assertThat(result.avaliadas()).isEqualTo(1);
        assertThat(result.reenviadas()).isEqualTo(1);
        assertThat(result.erros()).isEqualTo(0);

        ArgumentCaptor<String> tituloCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textoCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificacaoService).reinsistirEnvio(
                eq(existente),
                tituloCaptor.capture(),
                textoCaptor.capture()
        );
        assertThat(tituloCaptor.getValue()).startsWith("Lembrete:");
        assertThat(textoCaptor.getValue()).contains("emergência");
        assertThat(conexao.getUltimoLembreteInsistenteEm()).isNotNull();
    }

    @Test
    @DisplayName("usa intervalo de 12h como corte de elegibilidade")
    void shouldUseTwelveHourCutoff() {
        when(conexaoRepository.findPendentesParaLembreteInsistente(any(), any(), any()))
                .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusHours(12);
        service.processar();
        LocalDateTime after = LocalDateTime.now().minusHours(12);

        ArgumentCaptor<LocalDateTime> limiteCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(conexaoRepository).findPendentesParaLembreteInsistente(
                eq(StatusConexaoEnum.PENDENTE),
                any(Set.class),
                limiteCaptor.capture()
        );
        assertThat(limiteCaptor.getValue()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
        assertThat(properties.getInterval()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    @DisplayName("não reinsiste quando o usuário já abriu a notificação")
    void shouldSkipWhenNotificationAlreadyOpened() {
        UUID conexaoId = UUID.randomUUID();
        ConexaoEntity conexao = conexaoPendente(conexaoId);

        NotificacaoEntity existente = NotificacaoEntity.builder()
                .titulo("Nova solicitação de conexão")
                .texto("old")
                .tipo(TipoNotificacaoEnum.CONEXAO_SOLICITADA)
                .lidaEm(LocalDateTime.now().minusHours(1))
                .build();
        existente.setId(UUID.randomUUID());

        when(conexaoRepository.findPendentesParaLembreteInsistente(
                eq(StatusConexaoEnum.PENDENTE),
                any(Set.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(conexao));
        when(notificacaoRepository.findFirstByReferenciaIdAndTipoOrderByCreatedAtAsc(
                conexaoId,
                TipoNotificacaoEnum.CONEXAO_SOLICITADA
        )).thenReturn(Optional.of(existente));

        NotificacaoInsistenteJobResultDTO result = service.processar();

        assertThat(result.avaliadas()).isEqualTo(1);
        assertThat(result.reenviadas()).isEqualTo(0);
        assertThat(result.ignoradas()).isEqualTo(1);
        verify(notificacaoService, never()).reinsistirEnvio(any(), any(), any());
        verify(conexaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("não reinsiste quando o advogado já abriu a solicitação")
    void shouldSkipWhenConnectionAlreadyViewed() {
        UUID conexaoId = UUID.randomUUID();
        ConexaoEntity conexao = conexaoPendente(conexaoId);
        conexao.setVisualizadaEm(LocalDateTime.now().minusHours(1));

        when(conexaoRepository.findPendentesParaLembreteInsistente(
                eq(StatusConexaoEnum.PENDENTE),
                any(Set.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(conexao));

        NotificacaoInsistenteJobResultDTO result = service.processar();

        assertThat(result.reenviadas()).isEqualTo(0);
        assertThat(result.ignoradas()).isEqualTo(1);
        verify(notificacaoService, never()).reinsistirEnvio(any(), any(), any());
    }

    private ConexaoEntity conexaoPendente(UUID conexaoId) {
        UUID advogadoUsuarioId = UUID.randomUUID();
        UUID clienteUsuarioId = UUID.randomUUID();

        UsuarioEntity advUsuario = UsuarioEntity.builder()
                .nomeCompleto("Adv")
                .email("adv@test.com")
                .senha("x")
                .build();
        advUsuario.setId(advogadoUsuarioId);

        UsuarioEntity cliUsuario = UsuarioEntity.builder()
                .nomeCompleto("Maria")
                .email("cli@test.com")
                .senha("x")
                .build();
        cliUsuario.setId(clienteUsuarioId);

        AdvogadoEntity advogado = AdvogadoEntity.builder()
                .usuarioId(advogadoUsuarioId)
                .usuario(advUsuario)
                .nomeCompleto("Adv")
                .build();

        ClienteEntity cliente = ClienteEntity.builder()
                .usuarioId(clienteUsuarioId)
                .usuario(cliUsuario)
                .build();

        SolicitacaoEntity solicitacao = SolicitacaoEntity.builder()
                .titulo("Contrato")
                .urgencia(UrgenciaSolicitacaoEnum.EMERGENCIA)
                .build();

        ConexaoEntity conexao = ConexaoEntity.builder()
                .solicitacao(solicitacao)
                .advogado(advogado)
                .cliente(cliente)
                .status(StatusConexaoEnum.PENDENTE)
                .build();
        conexao.setId(conexaoId);
        conexao.setCreatedAt(LocalDateTime.now().minusDays(2));
        return conexao;
    }
}
