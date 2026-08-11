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
import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.advogado.AreaAtuacaoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.advogado.EspecialidadeInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.dto.advogado.PosGraduacaoInputDTO;
import com.laweact.mapper.AdvogadoMapper;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AdvogadoEspecialidadeEntity;
import com.laweact.model.entity.AdvogadoFormaCobrancaEntity;
import com.laweact.model.entity.AdvogadoModalidadeEntity;
import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;
import com.laweact.model.entity.EnderecoEntity;
import com.laweact.model.entity.EspecialidadeEntity;
import com.laweact.model.entity.FormaCobrancaEntity;
import com.laweact.model.entity.ModalidadeAtuacaoEntity;
import com.laweact.model.entity.OabEntity;
import com.laweact.model.entity.PosGraduacaoAdvogadoEntity;
import com.laweact.model.entity.SubespecialidadeEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;
import com.laweact.repository.AdvogadoEspecialidadeRepository;
import com.laweact.repository.AdvogadoFormaCobrancaRepository;
import com.laweact.repository.AdvogadoModalidadeRepository;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.AreaAtuacaoAdvogadoRepository;
import com.laweact.repository.EnderecoRepository;
import com.laweact.repository.EspecialidadeRepository;
import com.laweact.repository.FormaCobrancaRepository;
import com.laweact.repository.ModalidadeAtuacaoRepository;
import com.laweact.repository.OabRepository;
import com.laweact.repository.PosGraduacaoAdvogadoRepository;
import com.laweact.repository.SubespecialidadeRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AdvogadoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class AdvogadoServiceImp implements AdvogadoService {

    public static final String MODALIDADE_NENHUMA = "NENHUMA_DAS_ANTERIORES";
    public static final String ESPECIALIDADE_OUTROS = "OUTROS";
    private static final int MAX_OABS_SUPLEMENTARES = 5;

    private final UsuarioRepository usuarioRepository;
    private final AdvogadoRepository advogadoRepository;
    private final EnderecoRepository enderecoRepository;
    private final OabRepository oabRepository;
    private final AreaAtuacaoAdvogadoRepository areaAtuacaoAdvogadoRepository;
    private final ModalidadeAtuacaoRepository modalidadeAtuacaoRepository;
    private final AdvogadoModalidadeRepository advogadoModalidadeRepository;
    private final FormaCobrancaRepository formaCobrancaRepository;
    private final AdvogadoFormaCobrancaRepository advogadoFormaCobrancaRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final SubespecialidadeRepository subespecialidadeRepository;
    private final AdvogadoEspecialidadeRepository advogadoEspecialidadeRepository;
    private final PosGraduacaoAdvogadoRepository posGraduacaoAdvogadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final JwtUtil jwtUtil;
    private final AdvogadoMapper advogadoMapper;

    @Override
    @Transactional
    public CadastrarAdvogadoResponseDTO cadastrar(CadastrarAdvogadoInputDTO input) {
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

        validarLimiteOabsSuplementares(input.oabsSuplementares());

        OabInputDTO oabPrincipal = input.oabPrincipal();
        String oabNumero = oabPrincipal.numero().trim();
        String oabUf = oabPrincipal.uf().trim().toUpperCase();
        if (oabRepository.existsByNumeroAndUf(oabNumero, oabUf)) {
            throw new CustomError("OAB já cadastrada", HttpStatus.BAD_REQUEST);
        }

        List<ModalidadeAtuacaoEntity> modalidades = resolverModalidades(input.modalidades());
        validarRegrasModalidades(modalidades, input.especialidades());
        List<FormaCobrancaEntity> formasCobranca = resolverFormasCobranca(input.formasCobranca());

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
                .nomePai(blankToNull(input.nomePai()))
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
                .complemento(blankToNull(input.complemento()))
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

        List<AreaAtuacaoAdvogadoEntity> areasSalvas = salvarAreas(advogadoSalvo, input.areasAtuacao());
        List<AdvogadoModalidadeEntity> modalidadesSalvas = salvarModalidades(advogadoSalvo, modalidades);
        List<AdvogadoFormaCobrancaEntity> cobrancasSalvas = salvarFormasCobranca(advogadoSalvo, formasCobranca);
        List<AdvogadoEspecialidadeEntity> especialidadesSalvas = salvarEspecialidades(advogadoSalvo, input.especialidades());
        List<PosGraduacaoAdvogadoEntity> posGraduacoesSalvas = salvarPosGraduacoes(advogadoSalvo, input.posGraduacoes());

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);

        log.info("Advogado cadastrado: {}", email);
        return advogadoMapper.toCadastrarResponse(
                usuarioSalvo,
                advogadoSalvo,
                enderecoSalvo,
                oabsSalvas,
                areasSalvas,
                modalidadesSalvas,
                especialidadesSalvas,
                cobrancasSalvas,
                posGraduacoesSalvas,
                token
        );
    }

    public AdvogadoDetalheResponseDTO carregarDetalhe(java.util.UUID usuarioId) {
        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND));
        EnderecoEntity endereco = enderecoRepository.findByUsuario_Id(usuarioId).orElse(null);
        return advogadoMapper.toDetalheResponse(
                advogado,
                endereco,
                oabRepository.findByAdvogadoUsuarioId(usuarioId),
                areaAtuacaoAdvogadoRepository.findByAdvogadoUsuarioId(usuarioId),
                advogadoModalidadeRepository.findByAdvogadoUsuarioId(usuarioId),
                advogadoEspecialidadeRepository.findByAdvogadoUsuarioId(usuarioId),
                advogadoFormaCobrancaRepository.findByAdvogadoUsuarioId(usuarioId),
                posGraduacaoAdvogadoRepository.findByAdvogadoUsuarioId(usuarioId)
        );
    }

    private void validarLimiteOabsSuplementares(List<OabInputDTO> suplementares) {
        if (suplementares != null && suplementares.size() > MAX_OABS_SUPLEMENTARES) {
            throw new CustomError("São permitidas no máximo 5 OABs suplementares", HttpStatus.BAD_REQUEST);
        }
    }

    private List<ModalidadeAtuacaoEntity> resolverModalidades(List<String> codigos) {
        Set<String> unicos = new HashSet<>();
        List<ModalidadeAtuacaoEntity> modalidades = new ArrayList<>();
        for (String codigoRaw : codigos) {
            String codigo = codigoRaw.trim().toUpperCase();
            if (!unicos.add(codigo)) {
                continue;
            }
            ModalidadeAtuacaoEntity modalidade = modalidadeAtuacaoRepository.findByCodigo(codigo)
                    .orElseThrow(() -> new CustomError("Modalidade inválida: " + codigo, HttpStatus.BAD_REQUEST));
            modalidades.add(modalidade);
        }
        return modalidades;
    }

    private void validarRegrasModalidades(
            List<ModalidadeAtuacaoEntity> modalidades,
            List<EspecialidadeInputDTO> especialidades
    ) {
        boolean temNenhuma = modalidades.stream().anyMatch(m -> MODALIDADE_NENHUMA.equals(m.getCodigo()));
        boolean temOutras = modalidades.stream().anyMatch(m -> !MODALIDADE_NENHUMA.equals(m.getCodigo()));

        if (temNenhuma && temOutras) {
            throw new CustomError(
                    "A modalidade 'Nenhuma das anteriores' não pode ser combinada com outras modalidades",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (temNenhuma && (especialidades == null || especialidades.isEmpty())) {
            throw new CustomError(
                    "Informe ao menos uma especialidade ao selecionar 'Nenhuma das anteriores'",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private List<FormaCobrancaEntity> resolverFormasCobranca(List<String> codigos) {
        Set<String> unicos = new HashSet<>();
        List<FormaCobrancaEntity> formas = new ArrayList<>();
        for (String codigoRaw : codigos) {
            String codigo = codigoRaw.trim().toUpperCase();
            if (!unicos.add(codigo)) {
                continue;
            }
            FormaCobrancaEntity forma = formaCobrancaRepository.findByCodigo(codigo)
                    .orElseThrow(() -> new CustomError("Forma de cobrança inválida: " + codigo, HttpStatus.BAD_REQUEST));
            formas.add(forma);
        }
        return formas;
    }

    private List<AreaAtuacaoAdvogadoEntity> salvarAreas(AdvogadoEntity advogado, List<AreaAtuacaoInputDTO> areas) {
        List<AreaAtuacaoAdvogadoEntity> areasSalvas = new ArrayList<>();
        Set<String> areasUnicas = new HashSet<>();
        for (AreaAtuacaoInputDTO area : areas) {
            String estado = area.estado().trim().toUpperCase();
            String cidade = area.cidade().trim();
            String chave = estado + "|" + cidade.toLowerCase();
            if (!areasUnicas.add(chave)) {
                continue;
            }
            AreaAtuacaoAdvogadoEntity areaEntity = AreaAtuacaoAdvogadoEntity.builder()
                    .advogado(advogado)
                    .estado(estado)
                    .cidade(cidade)
                    .build();
            areasSalvas.add(areaAtuacaoAdvogadoRepository.save(areaEntity));
        }
        return areasSalvas;
    }

    private List<AdvogadoModalidadeEntity> salvarModalidades(
            AdvogadoEntity advogado,
            List<ModalidadeAtuacaoEntity> modalidades
    ) {
        List<AdvogadoModalidadeEntity> salvas = new ArrayList<>();
        for (ModalidadeAtuacaoEntity modalidade : modalidades) {
            AdvogadoModalidadeEntity entity = AdvogadoModalidadeEntity.builder()
                    .id(new AdvogadoModalidadeEntity.AdvogadoModalidadeId(
                            advogado.getUsuarioId(),
                            modalidade.getId()
                    ))
                    .advogado(advogado)
                    .modalidade(modalidade)
                    .build();
            salvas.add(advogadoModalidadeRepository.save(entity));
        }
        return salvas;
    }

    private List<AdvogadoFormaCobrancaEntity> salvarFormasCobranca(
            AdvogadoEntity advogado,
            List<FormaCobrancaEntity> formas
    ) {
        List<AdvogadoFormaCobrancaEntity> salvas = new ArrayList<>();
        for (FormaCobrancaEntity forma : formas) {
            AdvogadoFormaCobrancaEntity entity = AdvogadoFormaCobrancaEntity.builder()
                    .id(new AdvogadoFormaCobrancaEntity.AdvogadoFormaCobrancaId(
                            advogado.getUsuarioId(),
                            forma.getId()
                    ))
                    .advogado(advogado)
                    .formaCobranca(forma)
                    .build();
            salvas.add(advogadoFormaCobrancaRepository.save(entity));
        }
        return salvas;
    }

    private List<AdvogadoEspecialidadeEntity> salvarEspecialidades(
            AdvogadoEntity advogado,
            List<EspecialidadeInputDTO> especialidades
    ) {
        List<AdvogadoEspecialidadeEntity> salvas = new ArrayList<>();
        if (especialidades == null) {
            return salvas;
        }

        for (EspecialidadeInputDTO input : especialidades) {
            String livre = blankToNull(input.especialidadeLivre());
            String codigo = blankToNull(input.especialidadeCodigo());

            if (livre == null && codigo == null) {
                throw new CustomError(
                        "Informe o código da especialidade ou o texto livre",
                        HttpStatus.BAD_REQUEST
                );
            }

            EspecialidadeEntity especialidade = null;
            SubespecialidadeEntity subespecialidade = null;
            String subLivre = blankToNull(input.subespecialidadeLivre());

            if (codigo != null) {
                especialidade = especialidadeRepository.findByCodigo(codigo.toUpperCase())
                        .orElseThrow(() -> new CustomError(
                                "Especialidade inválida: " + codigo,
                                HttpStatus.BAD_REQUEST
                        ));

                String subCodigo = blankToNull(input.subespecialidadeCodigo());
                if (ESPECIALIDADE_OUTROS.equals(especialidade.getCodigo())) {
                    if (subLivre == null && subCodigo == null) {
                        throw new CustomError(
                                "Ao selecionar 'Outros', informe a subespecialidade",
                                HttpStatus.BAD_REQUEST
                        );
                    }
                }

                if (subCodigo != null) {
                    subespecialidade = subespecialidadeRepository
                            .findByEspecialidadeIdAndCodigo(especialidade.getId(), subCodigo.toUpperCase())
                            .orElseThrow(() -> new CustomError(
                                    "Subespecialidade inválida: " + subCodigo,
                                    HttpStatus.BAD_REQUEST
                            ));
                }
            }

            AdvogadoEspecialidadeEntity entity = AdvogadoEspecialidadeEntity.builder()
                    .advogado(advogado)
                    .especialidade(especialidade)
                    .subespecialidade(subespecialidade)
                    .especialidadeLivre(livre)
                    .subespecialidadeLivre(subLivre)
                    .build();
            salvas.add(advogadoEspecialidadeRepository.save(entity));
        }
        return salvas;
    }

    private List<PosGraduacaoAdvogadoEntity> salvarPosGraduacoes(
            AdvogadoEntity advogado,
            List<PosGraduacaoInputDTO> posGraduacoes
    ) {
        List<PosGraduacaoAdvogadoEntity> salvas = new ArrayList<>();
        if (posGraduacoes == null) {
            return salvas;
        }
        for (PosGraduacaoInputDTO input : posGraduacoes) {
            PosGraduacaoAdvogadoEntity entity = PosGraduacaoAdvogadoEntity.builder()
                    .advogado(advogado)
                    .nomeCurso(input.nomeCurso().trim())
                    .instituicao(input.instituicao().trim())
                    .anoFormacao(input.anoFormacao())
                    .build();
            salvas.add(posGraduacaoAdvogadoRepository.save(entity));
        }
        return salvas;
    }

    private OabEntity salvarOab(AdvogadoEntity advogado, OabInputDTO input, boolean principal) {
        OabEntity oab = OabEntity.builder()
                .advogado(advogado)
                .numero(input.numero().trim())
                .uf(input.uf().trim().toUpperCase())
                .dataExpedicao(input.dataExpedicao())
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
