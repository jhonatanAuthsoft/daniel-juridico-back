package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.laweact.config.ExpoPushProperties;
import com.laweact.config.exception.CustomError;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.DispositivoPushRepository;
import com.laweact.repository.NotificacaoRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ExpoPushClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService — marcar lidas por solicitação")
class NotificacaoLerPorSolicitacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private DispositivoPushRepository dispositivoPushRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitacaoRepository solicitacaoRepository;

    @Mock
    private ConexaoRepository conexaoRepository;

    @Mock
    private ExpoPushClient expoPushClient;

    private NotificacaoServiceImp service;

    private final UUID solicitacaoId = UUID.randomUUID();
    private final UUID clienteUsuarioId = UUID.randomUUID();
    private final UUID advogadoUsuarioId = UUID.randomUUID();
    private final UUID conexaoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ExpoPushProperties expoPushProperties = new ExpoPushProperties();
        expoPushProperties.setEnabled(false);
        service = new NotificacaoServiceImp(
                notificacaoRepository,
                dispositivoPushRepository,
                usuarioRepository,
                expoPushClient,
                expoPushProperties,
                solicitacaoRepository,
                conexaoRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("cliente dono marca notificações de todas as conexões da solicitação")
    void shouldMarkAllConnectionNotificationsForClient() {
        autenticar(cliente(clienteUsuarioId));
        UUID outraConexaoId = UUID.randomUUID();
        when(solicitacaoRepository.findById(solicitacaoId))
                .thenReturn(Optional.of(solicitacaoDoCliente(clienteUsuarioId)));
        when(conexaoRepository.findIdsBySolicitacaoId(solicitacaoId))
                .thenReturn(List.of(conexaoId, outraConexaoId));

        service.lerPorSolicitacao(solicitacaoId);

        verify(notificacaoRepository).marcarLidasPorReferencias(
                eq(clienteUsuarioId),
                eq(List.of(conexaoId, outraConexaoId)),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("advogado marca só as notificações da própria conexão")
    void shouldMarkOwnConnectionNotificationsForLawyer() {
        autenticar(advogado(advogadoUsuarioId));
        when(solicitacaoRepository.findById(solicitacaoId))
                .thenReturn(Optional.of(solicitacaoDoCliente(clienteUsuarioId)));
        ConexaoEntity conexao = new ConexaoEntity();
        conexao.setId(conexaoId);
        when(conexaoRepository.findBySolicitacao_IdAndAdvogado_UsuarioId(solicitacaoId, advogadoUsuarioId))
                .thenReturn(Optional.of(conexao));

        service.lerPorSolicitacao(solicitacaoId);

        verify(notificacaoRepository).marcarLidasPorReferencias(
                eq(advogadoUsuarioId),
                eq(List.of(conexaoId)),
                any(LocalDateTime.class)
        );
        verify(conexaoRepository, never()).findIdsBySolicitacaoId(any());
    }

    @Test
    @DisplayName("cliente sem conexões não chama o update")
    void shouldSkipUpdateWhenClientHasNoConnections() {
        autenticar(cliente(clienteUsuarioId));
        when(solicitacaoRepository.findById(solicitacaoId))
                .thenReturn(Optional.of(solicitacaoDoCliente(clienteUsuarioId)));
        when(conexaoRepository.findIdsBySolicitacaoId(solicitacaoId)).thenReturn(List.of());

        service.lerPorSolicitacao(solicitacaoId);

        verify(notificacaoRepository, never()).marcarLidasPorReferencias(any(), any(), any());
    }

    @Test
    @DisplayName("solicitação inexistente → 404")
    void shouldReturnNotFoundWhenSolicitationMissing() {
        autenticar(cliente(clienteUsuarioId));
        when(solicitacaoRepository.findById(solicitacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lerPorSolicitacao(solicitacaoId))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(notificacaoRepository, never()).marcarLidasPorReferencias(any(), any(), any());
    }

    @Test
    @DisplayName("cliente de outra solicitação → 403")
    void shouldForbidClientOfAnotherSolicitation() {
        autenticar(cliente(clienteUsuarioId));
        when(solicitacaoRepository.findById(solicitacaoId))
                .thenReturn(Optional.of(solicitacaoDoCliente(UUID.randomUUID())));

        assertThatThrownBy(() -> service.lerPorSolicitacao(solicitacaoId))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(notificacaoRepository, never()).marcarLidasPorReferencias(any(), any(), any());
    }

    @Test
    @DisplayName("advogado sem conexão na solicitação → 403")
    void shouldForbidLawyerWithoutConnection() {
        autenticar(advogado(advogadoUsuarioId));
        when(solicitacaoRepository.findById(solicitacaoId))
                .thenReturn(Optional.of(solicitacaoDoCliente(clienteUsuarioId)));
        when(conexaoRepository.findBySolicitacao_IdAndAdvogado_UsuarioId(solicitacaoId, advogadoUsuarioId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lerPorSolicitacao(solicitacaoId))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(notificacaoRepository, never()).marcarLidasPorReferencias(any(), any(), any());
    }

    private void autenticar(UsuarioEntity usuario) {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );
    }

    private static UsuarioEntity cliente(UUID id) {
        return usuario(id, "cli@laweact.com", PerfilUsuarioEnum.CLIENTE);
    }

    private static UsuarioEntity advogado(UUID id) {
        return usuario(id, "adv@laweact.com", PerfilUsuarioEnum.ADVOGADO);
    }

    private static UsuarioEntity usuario(UUID id, String email, PerfilUsuarioEnum perfil) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nomeCompleto("Usuário Teste")
                .email(email)
                .senha("x")
                .perfil(perfil)
                .status(StatusUsuarioEnum.ATIVO)
                .build();
        usuario.setId(id);
        return usuario;
    }

    private SolicitacaoEntity solicitacaoDoCliente(UUID donoId) {
        ClienteEntity cliente = ClienteEntity.builder()
                .usuarioId(donoId)
                .build();
        SolicitacaoEntity solicitacao = SolicitacaoEntity.builder()
                .cliente(cliente)
                .titulo("Contrato")
                .build();
        solicitacao.setId(solicitacaoId);
        return solicitacao;
    }
}
