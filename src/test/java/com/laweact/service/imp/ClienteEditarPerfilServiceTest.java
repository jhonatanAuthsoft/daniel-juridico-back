package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.laweact.dto.cliente.AtualizarDadosGeraisClienteInputDTO;
import com.laweact.dto.cliente.AtualizarEnderecoClienteInputDTO;
import com.laweact.dto.cliente.AtualizarPerfilPessoalClienteInputDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.mapper.ClienteMapper;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.PronomesEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.TipoDocumentoEnum;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.SessaoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService — editar dados cadastrais")
class ClienteEditarPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private EnderecoRepository enderecoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    @Mock
    private SessaoService sessaoService;
    @Mock
    private ClienteMapper clienteMapper;

    private ClienteServiceImp service;
    private final UUID usuarioId = UUID.randomUUID();
    private UsuarioEntity usuario;
    private ClienteEntity cliente;
    private EnderecoEntity endereco;

    @BeforeEach
    void setUp() {
        service = new ClienteServiceImp(
                usuarioRepository,
                clienteRepository,
                enderecoRepository,
                passwordEncoder,
                usuarioDetailsServiceImp,
                sessaoService,
                clienteMapper
        );

        usuario = UsuarioEntity.builder()
                .nomeCompleto("Maria Silva")
                .email("maria@laweact.com")
                .senha("x")
                .perfil(PerfilUsuarioEnum.CLIENTE)
                .status(StatusUsuarioEnum.ATIVO)
                .build();
        usuario.setId(usuarioId);

        cliente = ClienteEntity.builder()
                .usuario(usuario)
                .nomeCompleto("Maria Silva")
                .tipoDocumento(TipoDocumentoEnum.CPF)
                .numeroDocumento("52998224725")
                .rg("1234567")
                .pronomes(PronomesEnum.ELA)
                .profissao("Analista")
                .estadoCivil("casado")
                .faixaRenda("1500")
                .build();
        cliente.setUsuarioId(usuarioId);

        endereco = EnderecoEntity.builder()
                .usuario(usuario)
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .complemento("Apto 12")
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
    @DisplayName("atualiza nome no usuario e no perfil CPF")
    void shouldUpdateCpfName() {
        stubAuthenticatedCliente();
        when(clienteMapper.toDetalheResponse(cliente, endereco))
                .thenReturn(ClienteDetalheResponseDTO.builder().build());

        service.atualizarDadosGerais(
                AtualizarDadosGeraisClienteInputDTO.builder().nomeCompleto("Maria Silva Lima").build()
        );

        assertThat(usuario.getNomeCompleto()).isEqualTo("Maria Silva Lima");
        assertThat(cliente.getNomeCompleto()).isEqualTo("Maria Silva Lima");
        verify(usuarioRepository).save(usuario);
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("atualiza razão social quando o cliente é PJ")
    void shouldUpdateCnpjRazaoSocial() {
        cliente.setTipoDocumento(TipoDocumentoEnum.CNPJ);
        cliente.setRazaoSocial("Empresa Exemplo LTDA");
        cliente.setNumeroDocumento("11222333000181");
        stubAuthenticatedCliente();
        when(clienteMapper.toDetalheResponse(cliente, endereco))
                .thenReturn(ClienteDetalheResponseDTO.builder().build());

        service.atualizarDadosGerais(
                AtualizarDadosGeraisClienteInputDTO.builder()
                        .nomeCompleto("Empresa Exemplo Atualizada LTDA")
                        .build()
        );

        assertThat(usuario.getNomeCompleto()).isEqualTo("Empresa Exemplo Atualizada LTDA");
        assertThat(cliente.getNomeCompleto()).isEqualTo("Empresa Exemplo Atualizada LTDA");
        assertThat(cliente.getRazaoSocial()).isEqualTo("Empresa Exemplo Atualizada LTDA");
    }

    @Test
    @DisplayName("rejeita nome em branco")
    void shouldRejectBlankName() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisClienteInputDTO.builder().nomeCompleto("   ").build()
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
        stubAuthenticatedCliente();
        when(clienteMapper.toDetalheResponse(cliente, endereco))
                .thenReturn(ClienteDetalheResponseDTO.builder().build());

        service.atualizarEndereco(AtualizarEnderecoClienteInputDTO.builder()
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
    @DisplayName("atualiza perfil pessoal do CPF")
    void shouldUpdatePersonalProfile() {
        stubAuthenticatedCliente();
        when(clienteMapper.toDetalheResponse(cliente, endereco))
                .thenReturn(ClienteDetalheResponseDTO.builder().build());

        service.atualizarPerfilPessoal(AtualizarPerfilPessoalClienteInputDTO.builder()
                .pronomes(PronomesEnum.ELE)
                .profissao("Designer")
                .estadoCivil("solteiro")
                .faixaRenda("2500")
                .build());

        assertThat(cliente.getPronomes()).isEqualTo(PronomesEnum.ELE);
        assertThat(cliente.getProfissao()).isEqualTo("Designer");
        assertThat(cliente.getEstadoCivil()).isEqualTo("solteiro");
        assertThat(cliente.getFaixaRenda()).isEqualTo("2500");
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("atualiza área de atuação do PJ e limpa opcionais em branco")
    void shouldUpdateCnpjPersonalProfile() {
        cliente.setTipoDocumento(TipoDocumentoEnum.CNPJ);
        cliente.setAreaAtuacao("Tecnologia");
        stubAuthenticatedCliente();
        when(clienteMapper.toDetalheResponse(cliente, endereco))
                .thenReturn(ClienteDetalheResponseDTO.builder().build());

        service.atualizarPerfilPessoal(AtualizarPerfilPessoalClienteInputDTO.builder()
                .pronomes(PronomesEnum.NEUTRO)
                .areaAtuacao("Consultoria jurídica")
                .estadoCivil("  ")
                .faixaRenda("")
                .build());

        assertThat(cliente.getPronomes()).isEqualTo(PronomesEnum.NEUTRO);
        assertThat(cliente.getAreaAtuacao()).isEqualTo("Consultoria jurídica");
        assertThat(cliente.getEstadoCivil()).isNull();
        assertThat(cliente.getFaixaRenda()).isNull();
    }

    @Test
    @DisplayName("advogado não pode editar dados de cliente")
    void shouldForbidLawyer() {
        usuario.setPerfil(PerfilUsuarioEnum.ADVOGADO);
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizarDadosGerais(
                AtualizarDadosGeraisClienteInputDTO.builder().nomeCompleto("Não deveria").build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError error = (CustomError) ex;
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(error.getErrorCode()).isEqualTo("FORBIDDEN");
                });
    }

    private void stubAuthenticatedCliente() {
        when(usuarioRepository.findByEmail("maria@laweact.com")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.findByUsuario_Id(usuarioId)).thenReturn(Optional.of(endereco));
    }
}
