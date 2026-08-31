package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.laweact.dto.usuario.AtualizarSenhaInputDTO;
import com.laweact.dto.usuario.AtualizarSenhaResponseDTO;
import com.laweact.mapper.UsuarioMapper;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ArquivoService;
import com.laweact.service.SessaoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — alterar senha do autenticado")
class UsuarioAtualizarSenhaServiceTest {

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

    @InjectMocks
    private UsuarioServiceImp service;

    private UsuarioEntity usuario;

    @BeforeEach
    void setUp() {
        usuario = UsuarioEntity.builder()
                .nomeCompleto("Maria Silva")
                .email("maria@laweact.com")
                .senha("hashed-current")
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
    @DisplayName("atualiza a senha quando a senha atual confere")
    void shouldUpdatePasswordWhenCurrentMatches() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Secret12", "hashed-current")).thenReturn(true);
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("hashed-new");

        AtualizarSenhaResponseDTO response = service.atualizarSenhaDoUsuarioAutenticado(
                AtualizarSenhaInputDTO.builder()
                        .senhaAtual("Secret12")
                        .novaSenha("NovaSenha1")
                        .build()
        );

        assertThat(response.mensagem()).isEqualTo("Senha alterada com sucesso");
        assertThat(usuario.getSenha()).isEqualTo("hashed-new");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("rejeita senha atual incorreta")
    void shouldRejectWrongCurrentPassword() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPass1", "hashed-current")).thenReturn(false);

        assertThatThrownBy(() -> service.atualizarSenhaDoUsuarioAutenticado(
                AtualizarSenhaInputDTO.builder()
                        .senhaAtual("WrongPass1")
                        .novaSenha("NovaSenha1")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .hasMessageContaining("senha atual está incorreta")
                .extracting(error -> ((CustomError) error).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usuarioRepository, never()).save(usuario);
        verify(passwordEncoder, never()).encode("NovaSenha1");
    }

    @Test
    @DisplayName("rejeita nova senha igual à atual")
    void shouldRejectSamePassword() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Secret12", "hashed-current")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizarSenhaDoUsuarioAutenticado(
                AtualizarSenhaInputDTO.builder()
                        .senhaAtual("Secret12")
                        .novaSenha("Secret12")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .hasMessageContaining("diferente da senha atual")
                .extracting(error -> ((CustomError) error).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usuarioRepository, never()).save(usuario);
    }
}
