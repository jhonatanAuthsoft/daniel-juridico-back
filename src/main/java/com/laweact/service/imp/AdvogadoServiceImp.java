package com.laweact.service.imp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.advogado.AdvogadoPerfilPublicoResponseDTO;
import com.laweact.dto.advogado.AreaAtuacaoInputDTO;
import com.laweact.dto.advogado.AtualizarBiografiaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDadosGeraisAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDisponibilidadeAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarDocumentacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarEnderecoAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarFormasCobrancaAdvogadoInputDTO;
import com.laweact.dto.advogado.AtualizarGraduacaoAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.advogado.EspecialidadeInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.dto.advogado.PosGraduacaoInputDTO;
import com.laweact.dto.avaliacao.AvaliacaoItemResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoListagemResponseDTO;
import com.laweact.dto.avaliacao.CriarAvaliacaoInputDTO;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.mapper.AdvogadoMapper;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.AdvogadoEspecialidadeEntity;
import com.laweact.model.entity.AdvogadoFormaCobrancaEntity;
import com.laweact.model.entity.AdvogadoModalidadeEntity;
import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;
import com.laweact.model.entity.AvaliacaoAdvogadoEntity;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.ConexaoEntity;
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
import com.laweact.model.enums.StatusConexaoEnum;
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
import com.laweact.service.AdvogadoService;
import com.laweact.service.AssinaturaService;
import com.laweact.service.SessaoService;

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
    private final AvaliacaoAdvogadoRepository avaliacaoAdvogadoRepository;
    private final ClienteRepository clienteRepository;
    private final ConexaoRepository conexaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioDetailsServiceImp usuarioDetailsServiceImp;
    private final SessaoService sessaoService;
    private final AdvogadoMapper advogadoMapper;
    private final AssinaturaService assinaturaService;

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
                .atuacaoDesde(dataExpedicaoMaisAntiga(oabPrincipal, input.oabsSuplementares()))
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

        assinaturaService.criarAssinaturaPendenteParaAdvogado(usuarioSalvo);

        UserDetails userDetails = usuarioDetailsServiceImp.loadUserByUsername(email);
        var tokens = sessaoService.criar(usuarioSalvo, userDetails, null);

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
                tokens.token(),
                tokens.refreshToken()
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

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarDadosGerais(AtualizarDadosGeraisAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        String nome = requireNome(input.nomeCompleto());
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());

        usuario.setNomeCompleto(nome);
        advogado.setNomeCompleto(nome);
        usuarioRepository.save(usuario);
        advogadoRepository.save(advogado);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarEndereco(AtualizarEnderecoAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
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
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarFormasCobranca(AtualizarFormasCobrancaAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());
        if (input.formasCobranca() == null || input.formasCobranca().isEmpty()) {
            throw new CustomError("Informe ao menos uma forma de cobrança", HttpStatus.BAD_REQUEST);
        }

        List<FormaCobrancaEntity> formas = resolverFormasCobranca(input.formasCobranca());
        List<AdvogadoFormaCobrancaEntity> atuais = advogadoFormaCobrancaRepository.findByAdvogadoUsuarioId(usuario.getId());
        advogadoFormaCobrancaRepository.deleteAll(atuais);
        advogadoFormaCobrancaRepository.flush();
        salvarFormasCobranca(advogado, formas);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarBiografia(AtualizarBiografiaAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());

        advogado.setPronomeTratamento(input.pronomeTratamento());
        advogado.setBiografia(blankToNull(input.biografia()));
        advogadoRepository.save(advogado);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarDisponibilidade(AtualizarDisponibilidadeAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());

        advogado.setDisponibilidade(input.disponibilidade());
        advogadoRepository.save(advogado);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarDocumentacao(AtualizarDocumentacaoAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());

        validarLimiteOabsSuplementares(input.oabsSuplementares());
        garantirOabsDisponiveis(input.oabPrincipal(), input.oabsSuplementares(), usuario.getId());

        oabRepository.deleteAll(oabRepository.findByAdvogadoUsuarioId(usuario.getId()));
        oabRepository.flush();

        salvarOab(advogado, input.oabPrincipal(), true);
        if (input.oabsSuplementares() != null) {
            for (OabInputDTO suplementar : input.oabsSuplementares()) {
                salvarOab(advogado, suplementar, false);
            }
        }
        advogado.setAtuacaoDesde(dataExpedicaoMaisAntiga(input.oabPrincipal(), input.oabsSuplementares()));
        advogadoRepository.save(advogado);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoDetalheResponseDTO atualizarGraduacao(AtualizarGraduacaoAdvogadoInputDTO input) {
        UsuarioEntity usuario = obterAdvogadoAutenticado();
        AdvogadoEntity advogado = obterAdvogado(usuario.getId());

        advogado.setUniversidade(input.universidade().trim());
        advogado.setCurso(input.curso().trim());
        advogado.setAnoFormacao(input.anoFormacao());
        advogadoRepository.save(advogado);
        return carregarDetalhe(usuario.getId());
    }

    @Override
    @Transactional
    public AdvogadoPerfilPublicoResponseDTO obterPerfilPublico(UUID advogadoId) {
        UsuarioEntity solicitante = obterUsuarioAutenticado();
        if (solicitante.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError(
                    "Apenas clientes podem visualizar o perfil público do advogado",
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN"
            );
        }

        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(advogadoId)
                .orElseThrow(() -> new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND));
        if (advogado.getUsuario() == null
                || advogado.getUsuario().getStatus() != StatusUsuarioEnum.ATIVO) {
            throw new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND);
        }
        EnderecoEntity endereco = enderecoRepository.findByUsuario_Id(advogadoId).orElse(null);

        return advogadoMapper.toPerfilPublicoResponse(
                advogado,
                endereco,
                oabRepository.findByAdvogadoUsuarioId(advogadoId),
                areaAtuacaoAdvogadoRepository.findByAdvogadoUsuarioId(advogadoId),
                advogadoModalidadeRepository.findByAdvogadoUsuarioId(advogadoId),
                advogadoEspecialidadeRepository.findByAdvogadoUsuarioId(advogadoId),
                advogadoFormaCobrancaRepository.findByAdvogadoUsuarioId(advogadoId),
                posGraduacaoAdvogadoRepository.findByAdvogadoUsuarioId(advogadoId)
        );
    }

    @Override
    @Transactional
    public AvaliacoesPaginadas listarAvaliacoes(UUID advogadoId, int limit, int offset) {
        obterUsuarioAutenticado();
        if (!advogadoRepository.existsById(advogadoId)) {
            throw new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND);
        }

        int pageSize = limit > 0 ? limit : 10;
        int pageIndex = Math.max(offset, 0) / pageSize;
        // Sort definido na query (própria primeiro, depois mais recentes).
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);

        UUID usuarioAutenticadoId = obterUsuarioAutenticado().getId();
        Page<AvaliacaoAdvogadoEntity> page = avaliacaoAdvogadoRepository
                .findByAdvogadoOrderedWithOwnFirst(advogadoId, usuarioAutenticadoId, pageable);

        List<AvaliacaoItemResponseDTO> items = page.getContent().stream()
                .map(avaliacao -> toAvaliacaoItem(avaliacao, usuarioAutenticadoId))
                .toList();

        long total = avaliacaoAdvogadoRepository.countByAdvogado_UsuarioId(advogadoId);
        BigDecimal mediaRaw = avaliacaoAdvogadoRepository.mediaByAdvogadoId(advogadoId);
        BigDecimal media = (mediaRaw == null ? BigDecimal.ZERO : mediaRaw).setScale(1, RoundingMode.HALF_UP);

        AvaliacaoListagemResponseDTO data = AvaliacaoListagemResponseDTO.builder()
                .items(items)
                .mediaAvaliacoes(media)
                .totalAvaliacoes(total)
                .podeAvaliar(calcularPodeAvaliar(advogadoId, usuarioAutenticadoId))
                .build();

        return new AvaliacoesPaginadas(
                data,
                PaginationInfo.of(pageSize, pageIndex * pageSize, total)
        );
    }

    private boolean calcularPodeAvaliar(UUID advogadoId, UUID usuarioAutenticadoId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioAutenticadoId).orElse(null);
        if (usuario == null || usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            return false;
        }
        return temConexaoAceitaSemAvaliacao(usuarioAutenticadoId, advogadoId);
    }

    @Override
    @Transactional
    public AvaliacaoItemResponseDTO criarAvaliacao(UUID advogadoId, CriarAvaliacaoInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError(
                    "Apenas clientes podem avaliar advogados",
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN"
            );
        }

        AdvogadoEntity advogado = advogadoRepository.findById(advogadoId)
                .orElseThrow(() -> new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND));

        ClienteEntity cliente = clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new CustomError("Perfil de cliente não encontrado", HttpStatus.NOT_FOUND));

        BigDecimal nota = validarNotaAvaliacao(input.nota());
        String comentario = validarComentarioAvaliacao(input.comentario());

        ConexaoEntity conexao = resolverConexaoParaAvaliacao(usuario.getId(), advogadoId);

        AvaliacaoAdvogadoEntity salva = avaliacaoAdvogadoRepository.save(AvaliacaoAdvogadoEntity.builder()
                .advogado(advogado)
                .cliente(cliente)
                .conexao(conexao)
                .nota(nota)
                .comentario(comentario)
                .build());

        sincronizarAgregadosAvaliacao(advogado);

        log.info("Avaliação {} criada pelo cliente {} para advogado {}",
                salva.getId(), usuario.getEmail(), advogadoId);
        return toAvaliacaoItem(salva, usuario.getId());
    }

    private BigDecimal validarNotaAvaliacao(BigDecimal nota) {
        if (nota == null) {
            throw new CustomError("A nota é obrigatória", HttpStatus.BAD_REQUEST);
        }
        if (nota.compareTo(new BigDecimal("0.5")) < 0
                || nota.compareTo(new BigDecimal("5.0")) > 0) {
            throw new CustomError("A nota deve estar entre 0.5 e 5.0", HttpStatus.BAD_REQUEST);
        }
        BigDecimal times2 = nota.multiply(new BigDecimal("2"));
        if (times2.compareTo(times2.setScale(0, RoundingMode.FLOOR)) != 0) {
            throw new CustomError("A nota deve ser múltipla de 0.5", HttpStatus.BAD_REQUEST);
        }
        return nota.setScale(1, RoundingMode.HALF_UP);
    }

    private String validarComentarioAvaliacao(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        String trimmed = comentario.trim();
        if (trimmed.length() > 800) {
            throw new CustomError(
                    "O comentário deve ter no máximo 800 caracteres",
                    HttpStatus.BAD_REQUEST
            );
        }
        return trimmed;
    }

    private boolean temConexaoAceitaSemAvaliacao(UUID clienteId, UUID advogadoId) {
        return resolverConexaoAceitaSemAvaliacao(clienteId, advogadoId) != null;
    }

    private ConexaoEntity resolverConexaoParaAvaliacao(UUID clienteId, UUID advogadoId) {
        ConexaoEntity pendente = resolverConexaoAceitaSemAvaliacao(clienteId, advogadoId);
        if (pendente != null) {
            return pendente;
        }
        boolean temAceita = conexaoRepository.existsByCliente_UsuarioIdAndAdvogado_UsuarioIdAndStatus(
                clienteId,
                advogadoId,
                StatusConexaoEnum.ACEITA
        );
        if (!temAceita) {
            throw new CustomError(
                    "É necessário ter uma conexão aceita com o advogado para avaliar",
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN"
            );
        }
        throw new CustomError(
                "Você já avaliou esta conexão",
                HttpStatus.CONFLICT,
                "CONFLICT"
        );
    }

    private ConexaoEntity resolverConexaoAceitaSemAvaliacao(UUID clienteId, UUID advogadoId) {
        List<ConexaoEntity> aceitas = conexaoRepository
                .findByCliente_UsuarioIdAndAdvogado_UsuarioIdAndStatusOrderByCreatedAtAsc(
                        clienteId,
                        advogadoId,
                        StatusConexaoEnum.ACEITA
                );
        for (ConexaoEntity conexao : aceitas) {
            if (!avaliacaoAdvogadoRepository.existsByConexao_Id(conexao.getId())) {
                return conexao;
            }
        }
        return null;
    }

    private AvaliacaoItemResponseDTO toAvaliacaoItem(AvaliacaoAdvogadoEntity avaliacao, UUID usuarioAutenticadoId) {
        ClienteEntity cliente = avaliacao.getCliente();
        String nomeAvaliador = cliente.getRazaoSocial() != null && !cliente.getRazaoSocial().isBlank()
                ? cliente.getRazaoSocial()
                : cliente.getNomeCompleto();

        return AvaliacaoItemResponseDTO.builder()
                .id(avaliacao.getId())
                .nota(avaliacao.getNota())
                .comentario(avaliacao.getComentario())
                .nomeAvaliador(nomeAvaliador)
                .criadoEm(avaliacao.getCreatedAt())
                .propria(cliente.getUsuarioId().equals(usuarioAutenticadoId))
                .build();
    }

    @Override
    @Transactional
    public UUID excluirAvaliacao(UUID advogadoId, UUID avaliacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError(
                    "Apenas o cliente autor pode excluir a avaliação",
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN"
            );
        }

        AvaliacaoAdvogadoEntity avaliacao = avaliacaoAdvogadoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new CustomError("Avaliação não encontrada", HttpStatus.NOT_FOUND));

        if (!avaliacao.getAdvogado().getUsuarioId().equals(advogadoId)) {
            throw new CustomError("Avaliação não encontrada", HttpStatus.NOT_FOUND);
        }

        if (!avaliacao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError(
                    "Apenas o autor pode excluir esta avaliação",
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN"
            );
        }

        AdvogadoEntity advogado = avaliacao.getAdvogado();
        avaliacaoAdvogadoRepository.delete(avaliacao);
        sincronizarAgregadosAvaliacao(advogado);

        log.info("Avaliação {} excluída pelo cliente {}", avaliacaoId, usuario.getEmail());
        return avaliacaoId;
    }

    private void sincronizarAgregadosAvaliacao(AdvogadoEntity advogado) {
        UUID advogadoId = advogado.getUsuarioId();
        long total = avaliacaoAdvogadoRepository.countByAdvogado_UsuarioId(advogadoId);
        BigDecimal mediaRaw = avaliacaoAdvogadoRepository.mediaByAdvogadoId(advogadoId);
        BigDecimal media = (mediaRaw == null ? BigDecimal.ZERO : mediaRaw).setScale(2, RoundingMode.HALF_UP);
        advogado.setTotalAvaliacoes((int) total);
        advogado.setMediaAvaliacoes(media);
        advogadoRepository.save(advogado);
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private UsuarioEntity obterAdvogadoAutenticado() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            throw new CustomError("Apenas advogados podem editar estes dados", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        return usuario;
    }

    private AdvogadoEntity obterAdvogado(UUID usuarioId) {
        return advogadoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new CustomError("Perfil de advogado não encontrado", HttpStatus.NOT_FOUND));
    }

    private String requireNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new CustomError("O nome é obrigatório", HttpStatus.BAD_REQUEST);
        }
        return nomeCompleto.trim();
    }

    private void garantirOabsDisponiveis(
            OabInputDTO principal,
            List<OabInputDTO> suplementares,
            UUID advogadoId
    ) {
        Set<String> chaves = new HashSet<>();
        garantirOabDisponivel(principal, advogadoId, "OAB já cadastrada");
        chaves.add(chaveOab(principal));

        if (suplementares == null) {
            return;
        }
        for (OabInputDTO suplementar : suplementares) {
            String chave = chaveOab(suplementar);
            if (!chaves.add(chave)) {
                String numero = suplementar.numero().trim();
                String uf = suplementar.uf().trim().toUpperCase();
                throw new CustomError("OAB duplicada no cadastro: " + numero + "/" + uf, HttpStatus.BAD_REQUEST);
            }
            garantirOabDisponivel(
                    suplementar,
                    advogadoId,
                    "OAB suplementar já cadastrada: " + suplementar.numero().trim() + "/" + suplementar.uf().trim().toUpperCase()
            );
        }
    }

    private void garantirOabDisponivel(OabInputDTO input, UUID advogadoId, String mensagem) {
        String numero = input.numero().trim();
        String uf = input.uf().trim().toUpperCase();
        if (oabRepository.existsByNumeroAndUfAndAdvogadoUsuarioIdNot(numero, uf, advogadoId)) {
            throw new CustomError(mensagem, HttpStatus.BAD_REQUEST);
        }
    }

    private String chaveOab(OabInputDTO input) {
        return input.numero().trim() + "|" + input.uf().trim().toUpperCase();
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

    private LocalDate dataExpedicaoMaisAntiga(OabInputDTO principal, List<OabInputDTO> suplementares) {
        LocalDate oldest = principal.dataExpedicao();
        if (suplementares == null) {
            return oldest;
        }
        for (OabInputDTO suplementar : suplementares) {
            if (suplementar == null || suplementar.dataExpedicao() == null) {
                continue;
            }
            if (oldest == null || suplementar.dataExpedicao().isBefore(oldest)) {
                oldest = suplementar.dataExpedicao();
            }
        }
        return oldest;
    }

    private OabEntity salvarOab(AdvogadoEntity advogado, OabInputDTO input, boolean principal) {
        OabEntity oab = OabEntity.builder()
                .advogado(advogado)
                .numero(input.numero().trim())
                .uf(input.uf().trim().toUpperCase())
                .dataExpedicao(input.dataExpedicao())
                .principal(principal)
                .fotosUrls(normalizarFotosUrls(input.fotosUrls()))
                .statusValidacao(StatusVerificacaoEnum.PENDENTE)
                .build();
        return oabRepository.save(oab);
    }

    private List<String> normalizarFotosUrls(List<String> fotosUrls) {
        if (fotosUrls == null || fotosUrls.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalizadas = new ArrayList<>();
        for (String url : fotosUrls) {
            String value = blankToNull(url);
            if (value != null) {
                normalizadas.add(value);
            }
        }
        return normalizadas;
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
