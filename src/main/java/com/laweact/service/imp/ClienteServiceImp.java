package com.laweact.service.imp;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
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
    private final JwtUtil jwtUtil;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public CadastrarClienteResponseDTO cadastrar(CadastrarClienteInputDTO input) {
        if (!Boolean.TRUE.equals(input.aceiteTermos())) {
            throw new CustomError("É obrigatório aceitar os termos de uso", HttpStatus.BAD_REQUEST);
        }

        String email = input.email().toLowerCase().trim();
        String documento = normalizarDocumento(input.numeroDocumento());
        validarDocumento(input.tipoDocumento(), documento);

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new CustomError("E-mail já cadastrado", HttpStatus.BAD_REQUEST);
        }
        if (clienteRepository.existsByNumeroDocumento(documento)) {
            throw new CustomError("Documento já cadastrado", HttpStatus.BAD_REQUEST);
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nomeCompleto(input.nomeCompleto().trim())
                .email(email)
                .senha(passwordEncoder.encode(input.senha()))
                .perfil(PerfilUsuarioEnum.CLIENTE)
                .status(StatusUsuarioEnum.ATIVO)
                .telefone(input.telefone().trim())
                .tentativasLoginFalhas(0)
                .build();

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        ClienteEntity cliente = ClienteEntity.builder()
                .usuario(usuarioSalvo)
                .nomeCompleto(input.nomeCompleto().trim())
                .profissao(input.profissao().trim())
                .tipoDocumento(input.tipoDocumento())
                .numeroDocumento(documento)
                .rg(input.rg().trim())
                .dataNascimento(input.dataNascimento())
                .pronomes(input.pronomes())
                .fotoUrl(blankToNull(input.fotoUrl()))
                .faixaRenda(blankToNull(input.faixaRenda()))
                .estadoCivil(blankToNull(input.estadoCivil()))
                .build();

        ClienteEntity clienteSalvo = clienteRepository.save(cliente);

        EnderecoEntity endereco = EnderecoEntity.builder()
                .usuario(usuarioSalvo)
                .cep(normalizarCep(input.cep()))
                .logradouro(input.logradouro().trim())
                .numero(input.numero().trim())
                .bairro(input.bairro().trim())
                .cidade(input.cidade().trim())
                .estado(input.estado().trim().toUpperCase())
                .build();

        EnderecoEntity enderecoSalvo = enderecoRepository.save(endereco);

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);

        log.info("Cliente cadastrado: {}", email);
        return clienteMapper.toCadastrarResponse(usuarioSalvo, clienteSalvo, enderecoSalvo, token);
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
