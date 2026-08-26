package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.advogado.AtualizarBiografiaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDadosGeraisAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDocumentacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarEnderecoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarFormasCobrancaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarGraduacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.mapper.AdvogadoMapper;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.FormaCobrancaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.PronomeTratamentoEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;
import com.laweact.repository.AdvogadoEspecialidadeRepository;
import com.laweact.repository.AdvogadoFormaCobrancaRepository;
import com.laweact.repository.AdvogadoModalidadeRepository;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.AreaAtuacaoAdvogadoRepository;
import com.laweact.repository.AvaliacaoAdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.ConexaoRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.EspecialidadeRepository;
import com.laweact.repository.FormaCobrancaRepository;
import com.laweact.repository.ModalidadeAtuacaoRepository;
import com.laweact.repository.OabRepository;
import com.laweact.repository.PosGraduacaoAdvogadoRepository;
import com.laweact.repository.SubespecialidadeRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.SessaoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvogadoService — editar dados cadastrais")
class AdvogadoEditarPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AdvogadoRepository advogadoRepository;
    @Mock
    private EnderecoRepository enderecoRepository;
    @Mock
    private OabRepository oabRepository;
    @Mock
    private AreaAtuacaoAdvogadoRepository areaAtuacaoAdvogadoRepository;
    @Mock
    private ModalidadeAtuacaoRepository modalidadeAtuacaoRepository;
    @Mock
    private AdvogadoModalidadeRepository advogadoModalidadeRepository;
    @Mock
    private FormaCobrancaRepository formaCobrancaRepository;
    @Mock
    private AdvogadoFormaCobrancaRepository advogadoFormaCobrancaRepository;
    @Mock
    private EspecialidadeRepository especialidadeRepository;
    @Mock
    private SubespecialidadeRepository subespecialidadeRepository;
    @Mock
    private AdvogadoEspecialidadeRepository advogadoEspecialidadeRepository;
    @Mock
    private PosGraduacaoAdvogadoRepository posGraduacaoAdvogadoRepository;
    @Mock
    private AvaliacaoAdvogadoRepository avaliacaoAdvogadoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ConexaoRepository conexaoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    @Mock
    private SessaoService sessaoService;
    @Mock
    private AdvogadoMapper advogadoMapper;

    private AdvogadoServiceImp service;
    private final UUID usuarioId = UUID.randomUUID();
    private UsuarioEntity usuario;
    private AdvogadoEntity advogado;
    private EnderecoEntity endereco;

    @BeforeEach
    void setUp() {
        service = new AdvogadoServiceImp(
                usuarioRepository,
                advogadoRepository,
                enderecoRepository,
                oabRepository,
                areaAtuacaoAdvogadoRepository,
                modalidadeAtuacaoRepository,
                advogadoModalidadeRepository,
                formaCobrancaRepository,
                advogadoFormaCobrancaRepository,
                especialidadeRepository,
                subespecialidadeRepository,
                advogadoEspecialidadeRepository,
                posGraduacaoAdvogadoRepository,
                avaliacaoAdvogadoRepository,
                clienteRepository,
                conexaoRepository,
                passwordEncoder,
                usuarioDetailsServiceImp,
                sessaoService,
                advogadoMapper
        );

        usuario = UsuarioEntity.builder()
                .nomeCompleto("João Advogado")
                .email("joao@laweact.com")
                .senha("x")
                .perfil(PerfilUsuarioEnum.ADVOGADO)
                .status(StatusUsuarioEnum.ATIVO)
                .build();
        usuario.setId(usuarioId);

        advogado = AdvogadoEntity.builder()
                .usuario(usuario)
                .nomeCompleto("João Advogado")
                .rg("7654321")
                .rgOrgaoEmissor("SSP")
                .rgUf("SP")
                .cpf("39053344705")
                .nomeMae("Ana Advogada")
                .pronomeTratamento(PronomeTratamentoEnum.DOUTOR)
                .universidade("USP")
                .curso("Direito")
                .anoFormacao(2015)
                .atuacaoDesde(LocalDate.of(2016, 1, 10))
                .disponibilidade(DisponibilidadeAdvogadoEnum.DISPONIVEL)
                .statusVerificacao(StatusVerificacaoEnum.PENDENTE)
                .mediaAvaliacoes(BigDecimal.ZERO)
                .totalAvaliacoes(0)
                .build();
        advogado.setUsuarioId(usuarioId);

        endereco = EnderecoEntity.builder()
                .usuario(usuario)
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1500")
                .complemento("Conjunto 41")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("atualiza nome no usuario e no perfil do advogado")
    void shouldUpdateName() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarDadosGerais(
                AtualizarDadosGeraisAdvogadoInputDTO.builder().nomeCompleto("João Advogado Lima").build()
        );

        assertThat(usuario.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(advogado.getNomeCompleto()).isEqualTo("João Advogado Lima");
        verify(usuarioRepository).save(usuario);
        verify(advogadoRepository).save(advogado);
    }

    @Test
    @DisplayName("rejeita nome em branco")
    void shouldRejectBlankName() {
        when(usuarioRepository.findByEmail("joao@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisAdvogadoInputDTO.builder().nomeCompleto("   ").build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).containsIgnoringCase("nome");
                });
    }

    @Test
    @DisplayName("atualiza endereço e normaliza CEP e UF")
    void shouldUpdateAddress() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarEndereco(AtualizarEnderecoAdvogadoInputDTO.builder()
                .cep("01311100")
                .logradouro("Rua Augusta")
                .numero("200")
                .complemento("Cj 10")
                .bairro("Consolação")
                .cidade("São Paulo")
                .estado("sp")
                .build());

        assertThat(endereco.getCep()).isEqualTo("01311-100");
        assertThat(endereco.getLogradouro()).isEqualTo("Rua Augusta");
        assertThat(endereco.getNumero()).isEqualTo("200");
        assertThat(endereco.getComplemento()).isEqualTo("Cj 10");
        assertThat(endereco.getBairro()).isEqualTo("Consolação");
        assertThat(endereco.getEstado()).isEqualTo("SP");
        verify(enderecoRepository).save(endereco);
    }

    @Test
    @DisplayName("substitui formas de cobrança")
    void shouldReplaceBillingMethods() {
        stubAuthenticatedAdvogado();
        stubDetalhe();
        FormaCobrancaEntity forma = FormaCobrancaEntity.builder()
                .codigo("HONORARIOS_PERCENTUAIS")
                .nome("Honorários percentuais")
                .build();
        forma.setId(UUID.randomUUID());
        when(formaCobrancaRepository.findByCodigo("HONORARIOS_PERCENTUAIS")).thenReturn(Optional.of(forma));

        service.atualizarFormasCobranca(AtualizarFormasCobrancaAdvogadoInputDTO.builder()
                .formasCobranca(List.of("HONORARIOS_PERCENTUAIS"))
                .build());

        verify(advogadoFormaCobrancaRepository).deleteAll(List.of());
        verify(advogadoFormaCobrancaRepository).save(any());
    }

    @Test
    @DisplayName("atualiza pronome e biografia")
    void shouldUpdateBiography() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarBiografia(AtualizarBiografiaAdvogadoInputDTO.builder()
                .pronomeTratamento(PronomeTratamentoEnum.DOUTORA)
                .biografia("Advogada com atuação em direito civil.")
                .build());

        assertThat(advogado.getPronomeTratamento()).isEqualTo(PronomeTratamentoEnum.DOUTORA);
        assertThat(advogado.getBiografia()).isEqualTo("Advogada com atuação em direito civil.");
        verify(advogadoRepository).save(advogado);
    }

    @Test
    @DisplayName("rejeita OAB já cadastrada por outro advogado")
    void shouldRejectDuplicateOab() {
        stubAuthenticatedAdvogado();
        when(oabRepository.existsByNumeroAndUfAndAdvogadoUsuarioIdNot("810011", "SP", usuarioId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.atualizarDocumentacao(AtualizarDocumentacaoAdvogadoInputDTO.builder()
                .oabPrincipal(OabInputDTO.builder()
                        .numero("810011")
                        .uf("SP")
                        .dataExpedicao(LocalDate.of(2016, 3, 15))
                        .build())
                .build()))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).containsIgnoringCase("OAB");
                });
    }

    @Test
    @DisplayName("atualiza graduação")
    void shouldUpdateGraduation() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarGraduacao(AtualizarGraduacaoAdvogadoInputDTO.builder()
                .universidade("PUC-SP")
                .curso("Direito")
                .anoFormacao(2018)
                .build());

        assertThat(advogado.getUniversidade()).isEqualTo("PUC-SP");
        assertThat(advogado.getCurso()).isEqualTo("Direito");
        assertThat(advogado.getAnoFormacao()).isEqualTo(2018);
        verify(advogadoRepository).save(advogado);
    }

    @Test
    @DisplayName("cliente não pode editar dados de advogado")
    void shouldForbidCliente() {
        usuario.setPerfil(PerfilUsuarioEnum.CLIENTE);
        when(usuarioRepository.findByEmail("joao@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisAdvogadoInputDTO.builder().nomeCompleto("Não deveria").build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(error.getErrorCode()).isEqualTo("FORBIDDEN");
                });
    }

    private void stubAuthenticatedAdvogado() {
        when(usuarioRepository.findByEmail("joao@laweact.com")).thenReturn(Optional.of(usuario));
        when(advogadoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(advogado));
    }

    private void stubDetalhe() {
        when(enderecoRepository.findByUsuario_Id(usuarioId)).thenReturn(Optional.of(endereco));
        when(oabRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(areaAtuacaoAdvogadoRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(advogadoModalidadeRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(advogadoEspecialidadeRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(advogadoFormaCobrancaRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(posGraduacaoAdvogadoRepository.findByAdvogadoUsuarioId(usuarioId)).thenReturn(List.of());
        when(advogadoMapper.toDetalheResponse(
                eq(advogado),
                eq(endereco),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(AdvogadoDetalheResponseDTO.builder().build());
    }
}
