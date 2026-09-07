package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.mapper.UsuarioMapper;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ArquivoService;
import com.laweact.service.AssinaturaService;
import com.laweact.service.SessaoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — excluir conta do autenticado")
class UsuarioExcluirContaServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    @Mock
    private SessaoService sessaoService;
    @Mock
    private ClienteServiceImp clienteServiceImp;
    @Mock
    private AdvogadoServiceImp advogadoServiceImp;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AdvogadoRepository advogadoRepository;
    @Mock
    private ArquivoService arquivoService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AssinaturaService assinaturaService;
    @Mock
    private ConexaoRepository conexaoRepository;
    @Mock
    private SolicitacaoRepository solicitacaoRepository;

    @InjectMocks
    private UsuarioServiceImp service;

    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        usuario = UsuarioEntity.builder()
                .nomeCompleto("Maria Silva")
                .email("maria@laweact.com")
                .senha("hashed")
                .perfil(PerfilUsuarioEnum.CLIENTE)
                .status(StatusUsuarioEnum.ATIVO)
                .build();
        usuario.setId(UUID.randomUUID());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("encerra sessões e marca o usuário autenticado como excluído")
    void shouldEndSessionsAndSoftDeleteAuthenticatedUser() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));
        when(conexaoRepository.findByCliente_UsuarioIdOrAdvogado_UsuarioId(usuario.getId(), usuario.getId()))
                .thenReturn(List.of());
        when(solicitacaoRepository.findByCliente_UsuarioId(usuario.getId()))
                .thenReturn(List.of());

        service.excluirUsuarioAutenticado();

        verify(sessaoService).encerrarTodasDoUsuario(usuario.getId());
        verify(usuarioRepository).save(usuario);
        verify(usuarioRepository, never()).delete(usuario);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuarioEnum.EXCLUIDO);
        assertThat(usuario.getExcluidoEm()).isNotNull();
    }

    @Test
    @DisplayName("rejeita chamada sem autenticação")
    void shouldRejectUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.excluirUsuarioAutenticado())
                .isInstanceOf(CustomError.class)
                .hasMessageContaining("não autenticado")
                .extracting(error -> ((CustomError) error).getHttpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(usuarioRepository, never()).delete(usuario);
        verify(usuarioRepository, never()).save(usuario);
        verify(sessaoService, never()).encerrarTodasDoUsuario(usuario.getId());
    }
}
