package com.laweact.service.imp;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.cliente.AtualizarDadosGeraisClienteInputDTO;
import com.laweact.dto.cliente.AtualizarEnderecoClienteInputDTO;
import com.laweact.dto.cliente.AtualizarPerfilPessoalClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.mapper.ClienteMapper;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.TipoDocumentoEnum;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.ClienteService;
import com.laweact.service.SessaoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ClienteServiceImp implements ClienteService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final SessaoService sessaoService;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public CadastrarClienteResponseDTO cadastrar(CadastrarClienteInputDTO input) {
        validarCamposPorTipoDocumento(input);

        String email = input.email().toLowerCase().trim();
        String documento = normalizarDocumento(input.numeroDocumento());
        validarDocumento(input.tipoDocumento(), documento);

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new CustomError("E-mail já cadastrado", HttpStatus.BAD_REQUEST);
        }
        if (clienteRepository.existsByNumeroDocumento(documento)) {
            throw new CustomError("Documento já cadastrado", HttpStatus.BAD_REQUEST);
        }

        String nomeExibicao = resolverNomeExibicao(input);

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nomeCompleto(nomeExibicao)
                .email(email)
                .senha(passwordEncoder.encode(input.senha()))
                .perfil(PerfilUsuarioEnum.CLIENTE)
                .status(StatusUsuarioEnum.ATIVO)
                .telefone(input.telefone().trim())
                .tentativasLoginFalhas(0)
                .build();

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        ClienteEntity.ClienteEntityBuilder clienteBuilder = ClienteEntity.builder()
                .usuario(usuarioSalvo)
                .nomeCompleto(nomeExibicao)
                .tipoDocumento(input.tipoDocumento())
                .numeroDocumento(documento)
                .pronomes(input.pronomes())
                .fotoUrl(blankToNull(input.fotoUrl()))
                .faixaRenda(blankToNull(input.faixaRenda()))
                .estadoCivil(blankToNull(input.estadoCivil()));

        if (input.tipoDocumento() == TipoDocumentoEnum.CPF) {
            clienteBuilder
                    .profissao(input.profissao().trim())
                    .rg(input.rg().trim())
                    .rgOrgaoEmissor(input.rgOrgaoEmissor().trim())
                    .rgUf(input.rgUf().trim().toUpperCase())
                    .dataNascimento(input.dataNascimento());
        } else {
            clienteBuilder
                    .razaoSocial(input.razaoSocial().trim())
                    .areaAtuacao(input.areaAtuacao().trim());
        }

        ClienteEntity clienteSalvo = clienteRepository.save(clienteBuilder.build());

        EnderecoEntity endereco = EnderecoEntity.builder()
                .usuario(usuarioSalvo)
                .cep(normalizarCep(input.cep()))
                .logradouro(input.logradouro().trim())
                .numero(input.numero().trim())
                .complemento(blankToNull(input.complemento()))
                .bairro(input.bairro().trim())
                .cidade(input.cidade().trim())
                .estado(input.estado().trim().toUpperCase())
                .build();

        EnderecoEntity enderecoSalvo = enderecoRepository.save(endereco);

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        var tokens = sessaoService.criar(usuarioSalvo, userDetails, null);

        log.info("Cliente cadastrado: {}", email);
        return clienteMapper.toCadastrarResponse(
                usuarioSalvo, clienteSalvo, enderecoSalvo, tokens.token(), tokens.refreshToken()
        );
    }

    @Override
    @Transactional
    public ClienteDetalheResponseDTO atualizarDadosGerais(AtualizarDadosGeraisClienteInputDTO input) {
        UsuarioEntity usuario = obterClienteAutenticado();
        String nome = requireNome(input.nomeCompleto());

        ClienteEntity cliente = obterCliente(usuario.getId());
        usuario.setNomeCompleto(nome);
        cliente.setNomeCompleto(nome);
        if (cliente.getTipoDocumento() == TipoDocumentoEnum.CNPJ) {
            cliente.setRazaoSocial(nome);
        }

        usuarioRepository.save(usuario);
        clienteRepository.save(cliente);
        return detalhe(cliente, usuario.getId());
    }

    @Override
    @Transactional
    public ClienteDetalheResponseDTO atualizarEndereco(AtualizarEnderecoClienteInputDTO input) {
        UsuarioEntity usuario = obterClienteAutenticado();
        ClienteEntity cliente = obterCliente(usuario.getId());
        EnderecoEntity endereco = enderecoRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new CustomError("Endereço não encontrado", HttpStatus.NOT_FOUND));

        endereco.setCep(normalizarCep(input.cep()));
        endereco.setLogradouro(input.logradouro().trim());
        endereco.setNumero(input.numero().trim());
        endereco.setComplemento(blankToNull(input.complemento()));
        endereco.setBairro(input.bairro().trim());
        endereco.setCidade(input.cidade().trim());
        endereco.setEstado(input.estado().trim().toUpperCase());

        enderecoRepository.save(endereco);
        return clienteMapper.toDetalheResponse(cliente, endereco);
    }

    @Override
    @Transactional
    public ClienteDetalheResponseDTO atualizarPerfilPessoal(AtualizarPerfilPessoalClienteInputDTO input) {
        UsuarioEntity usuario = obterClienteAutenticado();
        ClienteEntity cliente = obterCliente(usuario.getId());

        cliente.setPronomes(input.pronomes());
        cliente.setEstadoCivil(blankToNull(input.estadoCivil()));
        cliente.setFaixaRenda(blankToNull(input.faixaRenda()));

        if (cliente.getTipoDocumento() == TipoDocumentoEnum.CNPJ) {
            String area = firstNonBlank(input.areaAtuacao(), input.profissao());
            if (isBlank(area)) {
                throw new CustomError("A área de atuação é obrigatória para CNPJ", HttpStatus.BAD_REQUEST);
            }
            cliente.setAreaAtuacao(area);
        } else {
            if (isBlank(input.profissao())) {
                throw new CustomError("A profissão é obrigatória para CPF", HttpStatus.BAD_REQUEST);
            }
            cliente.setProfissao(input.profissao().trim());
        }

        clienteRepository.save(cliente);
        return detalhe(cliente, usuario.getId());
    }

    public ClienteDetalheResponseDTO carregarDetalhe(java.util.UUID usuarioId) {
        return detalhe(obterCliente(usuarioId), usuarioId);
    }

    private ClienteDetalheResponseDTO detalhe(ClienteEntity cliente, java.util.UUID usuarioId) {
        EnderecoEntity endereco = enderecoRepository.findByUsuario_Id(usuarioId).orElse(null);
        return clienteMapper.toDetalheResponse(cliente, endereco);
    }

    private UsuarioEntity obterClienteAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        UsuarioEntity usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem editar estes dados", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        return usuario;
    }

    private ClienteEntity obterCliente(java.util.UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new CustomError("Perfil de cliente não encontrado", HttpStatus.NOT_FOUND));
    }

    private String requireNome(String nomeCompleto) {
        if (isBlank(nomeCompleto)) {
            throw new CustomError("O nome é obrigatório", HttpStatus.BAD_REQUEST);
        }
        return nomeCompleto.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary.trim();
        }
        if (!isBlank(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private void validarCamposPorTipoDocumento(CadastrarClienteInputDTO input) {
        if (input.tipoDocumento() == TipoDocumentoEnum.CPF) {
            if (isBlank(input.nomeCompleto())) {
                throw new CustomError("O nome completo é obrigatório para CPF", HttpStatus.BAD_REQUEST);
            }
            if (isBlank(input.rg())) {
                throw new CustomError("O RG é obrigatório para CPF", HttpStatus.BAD_REQUEST);
            }
            if (isBlank(input.rgOrgaoEmissor())) {
                throw new CustomError("O órgão emissor do RG é obrigatório para CPF", HttpStatus.BAD_REQUEST);
            }
            if (isBlank(input.rgUf()) || input.rgUf().trim().length() != 2) {
                throw new CustomError("A UF do RG é obrigatória para CPF", HttpStatus.BAD_REQUEST);
            }
            if (input.dataNascimento() == null) {
                throw new CustomError("A data de nascimento é obrigatória para CPF", HttpStatus.BAD_REQUEST);
            }
            if (isBlank(input.profissao())) {
                throw new CustomError("A profissão é obrigatória para CPF", HttpStatus.BAD_REQUEST);
            }
            return;
        }

        if (input.tipoDocumento() == TipoDocumentoEnum.CNPJ) {
            if (isBlank(input.razaoSocial())) {
                throw new CustomError("A razão social é obrigatória para CNPJ", HttpStatus.BAD_REQUEST);
            }
            if (isBlank(input.areaAtuacao())) {
                throw new CustomError("A área de atuação é obrigatória para CNPJ", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String resolverNomeExibicao(CadastrarClienteInputDTO input) {
        if (input.tipoDocumento() == TipoDocumentoEnum.CNPJ) {
            if (!isBlank(input.nomeCompleto())) {
                return input.nomeCompleto().trim();
            }
            return input.razaoSocial().trim();
        }
        return input.nomeCompleto().trim();
    }

    private void validarDocumento(TipoDocumentoEnum tipo, String documento) {
        if (tipo == TipoDocumentoEnum.CPF && documento.length() != 11) {
            throw new CustomError("CPF deve conter 11 dígitos", HttpStatus.BAD_REQUEST);
        }
        if (tipo == TipoDocumentoEnum.CNPJ && documento.length() != 14) {
            throw new CustomError("CNPJ deve conter 14 dígitos", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizarDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }

    private String normalizarCep(String cep) {
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new CustomError("CEP inválido", HttpStatus.BAD_REQUEST);
        }
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
