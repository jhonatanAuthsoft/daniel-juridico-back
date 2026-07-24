package com.laweact.service.imp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.advogado.AreaAtuacaoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.mapper.AdvogadoMapper;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.OabEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.AreaAtuacaoAdvogadoRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.OabRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AdvogadoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class AdvogadoServiceImp implements AdvogadoService {

    private final UsuarioRepository usuarioRepository;
    private final AdvogadoRepository advogadoRepository;
    private final EnderecoRepository enderecoRepository;
    private final OabRepository oabRepository;
    private final AreaAtuacaoAdvogadoRepository areaAtuacaoAdvogadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final JwtUtil jwtUtil;
    private final AdvogadoMapper advogadoMapper;

    @Override
    @Transactional
    public CadastrarAdvogadoResponseDTO cadastrar(CadastrarAdvogadoInputDTO input) {
        if (!Boolean.TRUE.equals(input.aceiteTermos())) {
            throw new CustomError("É obrigatório aceitar os termos de uso", HttpStatus.BAD_REQUEST);
        }

        String email = input.email().toLowerCase().trim();
        String cpf = normalizarDocumento(input.cpf());
        if (cpf.length() != 11) {
            throw new CustomError("CPF deve conter 11 dígitos", HttpStatus.BAD_REQUEST);
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new CustomError("E-mail já cadastrado", HttpStatus.BAD_REQUEST);
        }
        if (advogadoRepository.existsByCpf(cpf)) {
            throw new CustomError("CPF já cadastrado", HttpStatus.BAD_REQUEST);
        }

        OabInputDTO oabPrincipal = input.oabPrincipal();
        String oabNumero = oabPrincipal.numero().trim();
        String oabUf = oabPrincipal.uf().trim().toUpperCase();
        if (oabRepository.existsByNumeroAndUf(oabNumero, oabUf)) {
            throw new CustomError("OAB já cadastrada", HttpStatus.BAD_REQUEST);
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nomeCompleto(input.nomeCompleto().trim())
                .email(email)
                .senha(passwordEncoder.encode(input.senha()))
                .perfil(PerfilUsuarioEnum.ADVOGADO)
                .status(StatusUsuarioEnum.ATIVO)
                .telefone(input.telefone().trim())
                .tentativasLoginFalhas(0)
                .build();

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        AdvogadoEntity advogado = AdvogadoEntity.builder()
                .usuario(usuarioSalvo)
                .nomeCompleto(input.nomeCompleto().trim())
                .nomeSocial(blankToNull(input.nomeSocial()))
                .rg(input.rg().trim())
                .rgOrgaoEmissor(input.rgOrgaoEmissor().trim())
                .rgUf(input.rgUf().trim().toUpperCase())
                .cpf(cpf)
                .nomePai(input.nomePai().trim())
                .nomeMae(input.nomeMae().trim())
                .pronomeTratamento(input.pronomeTratamento())
                .fotoUrl(blankToNull(input.fotoUrl()))
                .universidade(input.universidade().trim())
                .curso(input.curso().trim())
                .anoFormacao(input.anoFormacao())
                .atuacaoDesde(input.atuacaoDesde())
                .biografia(blankToNull(input.biografia()))
                .disponibilidade(DisponibilidadeAdvogadoEnum.DISPONIVEL)
                .statusVerificacao(StatusVerificacaoEnum.PENDENTE)
                .mediaAvaliacoes(BigDecimal.ZERO)
                .totalAvaliacoes(0)
                .build();

        AdvogadoEntity advogadoSalvo = advogadoRepository.save(advogado);

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

        List<OabEntity> oabsSalvas = new ArrayList<>();
        oabsSalvas.add(salvarOab(advogadoSalvo, oabPrincipal, true));

        if (input.oabsSuplementares() != null) {
            for (OabInputDTO suplementar : input.oabsSuplementares()) {
                String num = suplementar.numero().trim();
                String uf = suplementar.uf().trim().toUpperCase();
                if (oabRepository.existsByNumeroAndUf(num, uf)) {
                    throw new CustomError("OAB suplementar já cadastrada: " + num + "/" + uf, HttpStatus.BAD_REQUEST);
                }
                oabsSalvas.add(salvarOab(advogadoSalvo, suplementar, false));
            }
        }

        List<AreaAtuacaoAdvogadoEntity> areasSalvas = new ArrayList<>();
        Set<String> areasUnicas = new HashSet<>();
        for (AreaAtuacaoInputDTO area : input.areasAtuacao()) {
            String estado = area.estado().trim().toUpperCase();
            String cidade = area.cidade().trim();
            String chave = estado + "|" + cidade.toLowerCase();
            if (!areasUnicas.add(chave)) {
                continue;
            }
            AreaAtuacaoAdvogadoEntity areaEntity = AreaAtuacaoAdvogadoEntity.builder()
                    .advogado(advogadoSalvo)
                    .estado(estado)
                    .cidade(cidade)
                    .build();
            areasSalvas.add(areaAtuacaoAdvogadoRepository.save(areaEntity));
        }

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);

        log.info("Advogado cadastrado: {}", email);
        return advogadoMapper.toCadastrarResponse(
                usuarioSalvo,
                advogadoSalvo,
                enderecoSalvo,
                oabsSalvas,
                areasSalvas,
                token
        );
    }

    private OabEntity salvarOab(AdvogadoEntity advogado, OabInputDTO input, boolean principal) {
        OabEntity oab = OabEntity.builder()
                .advogado(advogado)
                .numero(input.numero().trim())
                .uf(input.uf().trim().toUpperCase())
                .principal(principal)
                .fotoFrenteUrl(blankToNull(input.fotoFrenteUrl()))
                .fotoVersoUrl(blankToNull(input.fotoVersoUrl()))
                .statusValidacao(StatusVerificacaoEnum.PENDENTE)
                .build();
        return oabRepository.save(oab);
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
