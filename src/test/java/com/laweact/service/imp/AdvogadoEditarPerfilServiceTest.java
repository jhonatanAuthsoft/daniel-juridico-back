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
import com.laweact.dto.advogado.AreaAtuacaoInputDTO;
import com.laweact.dto.advogado.AtualizarAreasAtuacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarBiografiaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDisponibilidadeAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDadosGeraisAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDocumentacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarEnderecoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarFormasCobrancaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarGraduacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.dto.advogado.PosGraduacaoInputDTO;
import com.laweact.mapper.AdvogadoMapper;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.FormaCobrancaEntity;
import com.laweact.model.entity.PosGraduacaoAdvogadoEntity;
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
import com.laweact.service.AssinaturaService;
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
    @Mock
    private AssinaturaService assinaturaService;

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
                advogadoMapper,
                assinaturaService
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
                AtualizarDadosGeraisAdvogadoInputDTO.builder()
                        .nomeCompleto("João Advogado Lima")
                        .telefone("(11) 97777-6666")
                        .dataNascimento(LocalDate.of(1988, 3, 12))
                        .build()
        );

        assertThat(usuario.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(usuario.getTelefone()).isEqualTo("11977776666");
        assertThat(advogado.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(advogado.getDataNascimento()).isEqualTo(LocalDate.of(1988, 3, 12));
        verify(usuarioRepository).save(usuario);
        verify(advogadoRepository).save(advogado);
    }

    @Test
    @DisplayName("rejeita nome em branco")
    void shouldRejectBlankName() {
        when(usuarioRepository.findByEmail("joao@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisAdvogadoInputDTO.builder()
                        .nomeCompleto("   ")
                        .telefone("11988887777")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).containsIgnoringCase("nome");
                });
    }

    @Test
    @DisplayName("rejeita telefone em branco")
    void shouldRejectBlankPhone() {
        when(usuarioRepository.findByEmail("joao@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisAdvogadoInputDTO.builder()
                        .nomeCompleto("João Advogado")
                        .telefone("   ")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).containsIgnoringCase("telefone");
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
    @DisplayName("substitui áreas de atuação")
    void shouldReplaceServiceAreas() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarAreasAtuacao(AtualizarAreasAtuacaoAdvogadoInputDTO.builder()
                .areasAtuacao(List.of(
                        AreaAtuacaoInputDTO.builder().estado("sp").cidade("Adamantina").build(),
                        AreaAtuacaoInputDTO.builder().estado("SP").cidade("Avaré").build()
                ))
                .build());

        verify(areaAtuacaoAdvogadoRepository).deleteAll(List.of());
        verify(areaAtuacaoAdvogadoRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("rejeita áreas de atuação vazias")
    void shouldRejectEmptyServiceAreas() {
        stubAuthenticatedAdvogado();

        assertThatThrownBy(() -> service.atualizarAreasAtuacao(AtualizarAreasAtuacaoAdvogadoInputDTO.builder()
                .areasAtuacao(List.of())
                .build()))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).containsIgnoringCase("área de atuação");
                });
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
                        .fotosUrls(List.of("tmp/oab/frente.jpg", "tmp/oab/verso.jpg"))
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
    @DisplayName("recalcula atuacaoDesde pela OAB mais antiga ao editar documentação")
    void shouldUpdateAtuacaoDesdeFromOldestOab() {
        stubAuthenticatedAdvogado();
        stubDetalhe();
        when(oabRepository.existsByNumeroAndUfAndAdvogadoUsuarioIdNot("123456", "SP", usuarioId))
                .thenReturn(false);
        when(oabRepository.existsByNumeroAndUfAndAdvogadoUsuarioIdNot("654321", "RJ", usuarioId))
                .thenReturn(false);
        when(oabRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizarDocumentacao(AtualizarDocumentacaoAdvogadoInputDTO.builder()
                .oabPrincipal(OabInputDTO.builder()
                        .numero("123456")
                        .uf("SP")
                        .dataExpedicao(LocalDate.of(2018, 1, 10))
                        .fotosUrls(List.of("tmp/oab/frente.jpg", "tmp/oab/verso.jpg"))
                        .build())
                .oabsSuplementares(List.of(OabInputDTO.builder()
                        .numero("654321")
                        .uf("RJ")
                        .dataExpedicao(LocalDate.of(2016, 3, 15))
                        .fotosUrls(List.of("tmp/oab/frente2.jpg", "tmp/oab/verso2.jpg"))
                        .build()))
                .build());

        assertThat(advogado.getAtuacaoDesde()).isEqualTo(LocalDate.of(2016, 3, 15));
        verify(advogadoRepository).save(advogado);
    }

    @Test
    @DisplayName("atualiza disponibilidade do perfil")
    void shouldUpdateAvailability() {
        stubAuthenticatedAdvogado();
        stubDetalhe();

        service.atualizarDisponibilidade(AtualizarDisponibilidadeAdvogadoInputDTO.builder()
                .disponibilidade(DisponibilidadeAdvogadoEnum.INDISPONIVEL)
                .build());

        assertThat(advogado.getDisponibilidade()).isEqualTo(DisponibilidadeAdvogadoEnum.INDISPONIVEL);
        verify(advogadoRepository).save(advogado);
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
        verify(posGraduacaoAdvogadoRepository).deleteAll(List.of());
    }

    @Test
    @DisplayName("substitui pós-graduações ao atualizar formação")
    void shouldReplacePostgraduates() {
        stubAuthenticatedAdvogado();
        stubDetalhe();
        PosGraduacaoAdvogadoEntity atual = PosGraduacaoAdvogadoEntity.builder()
                .advogado(advogado)
                .nomeCurso("MBA")
                .instituicao("FIA")
                .anoFormacao(2019)
                .build();
        when(posGraduacaoAdvogadoRepository.findByAdvogadoUsuarioId(usuarioId))
                .thenReturn(List.of(atual))
                .thenReturn(List.of());

        service.atualizarGraduacao(AtualizarGraduacaoAdvogadoInputDTO.builder()
                .universidade("PUC-SP")
                .curso("Direito")
                .anoFormacao(2018)
                .posGraduacoes(List.of(PosGraduacaoInputDTO.builder()
                        .nomeCurso("LLM Direito Digital")
                        .instituicao("FGV")
                        .anoFormacao(2020)
                        .build()))
                .build());

        verify(posGraduacaoAdvogadoRepository).deleteAll(List.of(atual));
        verify(posGraduacaoAdvogadoRepository).flush();
        verify(posGraduacaoAdvogadoRepository).save(any());
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
