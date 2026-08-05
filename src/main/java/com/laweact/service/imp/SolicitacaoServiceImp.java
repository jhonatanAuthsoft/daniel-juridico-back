package com.laweact.service.imp;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoListagemItemDTO;
import com.laweact.dto.solicitacao.SolicitacaoListagemResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoMatchResponseDTO;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.EspecialidadeEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.SolicitacaoMatchEntity;
import com.laweact.model.entity.SubespecialidadeEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.EspecialidadeRepository;
import com.laweact.repository.SolicitacaoMatchRepository;
import com.laweact.repository.SolicitacaoRepository;
import com.laweact.repository.SubespecialidadeRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.MatchingService;
import com.laweact.service.SolicitacaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SolicitacaoServiceImp implements SolicitacaoService {

    private static final EnumSet<StatusSolicitacaoEnum> STATUS_NAO_CANCELAVEIS = EnumSet.of(
            StatusSolicitacaoEnum.CANCELADA,
            StatusSolicitacaoEnum.ENCERRADA
    );

    private final SolicitacaoRepository solicitacaoRepository;
    private final SolicitacaoMatchRepository solicitacaoMatchRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final SubespecialidadeRepository subespecialidadeRepository;
    private final MatchingService matchingService;

    @Override
    @Transactional
    public CriarSolicitacaoResponseDTO criar(CriarSolicitacaoInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem criar solicitações", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        ClienteEntity cliente = clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new CustomError("Perfil de cliente não encontrado", HttpStatus.NOT_FOUND));

        EspecialidadeEntity especialidade = especialidadeRepository
                .findByCodigo(input.especialidadeCodigo().trim().toUpperCase())
                .orElseThrow(() -> new CustomError("Especialidade inválida", HttpStatus.BAD_REQUEST, "INVALID_SPECIALTY"));

        String subCodigo = blankToNull(input.subespecialidadeCodigo());
        if (subCodigo != null) {
            SubespecialidadeEntity sub = subespecialidadeRepository
                    .findByEspecialidadeIdAndCodigo(especialidade.getId(), subCodigo.toUpperCase())
                    .orElseThrow(() -> new CustomError(
                            "Subespecialidade inválida para a especialidade informada",
                            HttpStatus.BAD_REQUEST,
                            "INVALID_SUBSPECIALTY"
                    ));
            subCodigo = sub.getCodigo();
        }

        SolicitacaoEntity entity = SolicitacaoEntity.builder()
                .cliente(cliente)
                .titulo(input.titulo().trim())
                .modalidade(input.modalidade())
                .especialidadeCodigo(especialidade.getCodigo())
                .subespecialidadeCodigo(subCodigo)
                .uf(input.uf().trim().toUpperCase())
                .cidade(input.cidade().trim())
                .urgencia(input.urgencia())
                .descricao(input.descricao().trim())
                .formaCobranca(input.formaCobranca())
                .experienciaMinimaMeses(input.experienciaMinimaMeses())
                .status(StatusSolicitacaoEnum.ABERTA)
                .build();

        SolicitacaoEntity salva = solicitacaoRepository.save(entity);
        List<SolicitacaoMatchEntity> matches = matchingService.gerarMatches(salva);

        log.info("Solicitação {} criada pelo cliente {}", salva.getId(), usuario.getEmail());
        return toResponse(salva, matches.size());
    }

    @Override
    @Transactional
    public CriarSolicitacaoResponseDTO buscarDoClienteAutenticado(UUID solicitacaoId) {
        SolicitacaoEntity solicitacao = obterSolicitacaoDoClienteAutenticado(solicitacaoId);
        long totalMatches = solicitacaoMatchRepository.countBySolicitacao_Id(solicitacao.getId());
        return toResponse(solicitacao, (int) totalMatches);
    }

    @Override
    @Transactional
    public CriarSolicitacaoResponseDTO cancelarDoClienteAutenticado(UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem cancelar solicitações", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        SolicitacaoEntity solicitacao = obterSolicitacaoDoClienteAutenticado(solicitacaoId);
        if (STATUS_NAO_CANCELAVEIS.contains(solicitacao.getStatus())) {
            throw new CustomError(
                    "Solicitação não pode ser cancelada no status atual",
                    HttpStatus.CONFLICT,
                    "INVALID_STATUS"
            );
        }

        solicitacao.setStatus(StatusSolicitacaoEnum.CANCELADA);
        SolicitacaoEntity salva = solicitacaoRepository.save(solicitacao);
        long totalMatches = solicitacaoMatchRepository.countBySolicitacao_Id(salva.getId());

        log.info("Solicitação {} cancelada pelo cliente {}", salva.getId(), usuario.getEmail());
        return toResponse(salva, (int) totalMatches);
    }

    @Override
    @Transactional
    public List<SolicitacaoMatchResponseDTO> listarMatches(UUID solicitacaoId) {
        obterSolicitacaoDoClienteAutenticado(solicitacaoId);

        return solicitacaoMatchRepository.findRankingBySolicitacaoId(solicitacaoId)
                .stream()
                .map(this::toMatchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ListagemPaginada listarDoClienteAutenticado(int limit, int offset, StatusSolicitacaoEnum status) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.CLIENTE) {
            throw new CustomError("Apenas clientes podem listar solicitações", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        int pageSize = limit > 0 ? limit : 10;
        int pageIndex = Math.max(offset, 0) / pageSize;
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);

        Page<SolicitacaoEntity> page = status == null
                ? solicitacaoRepository.findByCliente_UsuarioIdOrderByCreatedAtDesc(usuario.getId(), pageable)
                : solicitacaoRepository.findByCliente_UsuarioIdAndStatusOrderByCreatedAtDesc(
                        usuario.getId(),
                        status,
                        pageable
                );

        List<SolicitacaoEntity> solicitacoes = page.getContent();
        Map<UUID, Long> matchesPorSolicitacao = contarMatches(solicitacoes);
        Map<String, String> nomesEspecialidade = carregarNomesEspecialidade(solicitacoes);

        List<SolicitacaoListagemItemDTO> items = solicitacoes.stream()
                .map(s -> SolicitacaoListagemItemDTO.builder()
                        .id(s.getId())
                        .status(s.getStatus())
                        .urgencia(s.getUrgencia())
                        .titulo(s.getTitulo())
                        .descricao(s.getDescricao())
                        .dataAbertura(s.getCreatedAt())
                        .especialidadeCodigo(s.getEspecialidadeCodigo())
                        .especialidade(nomesEspecialidade.getOrDefault(
                                s.getEspecialidadeCodigo(),
                                s.getEspecialidadeCodigo()
                        ))
                        .totalMatches(matchesPorSolicitacao.getOrDefault(s.getId(), 0L).intValue())
                        .build())
                .toList();

        SolicitacaoListagemResponseDTO data = SolicitacaoListagemResponseDTO.builder()
                .items(items)
                .contagemPorStatus(carregarContagemPorStatus(usuario.getId()))
                .build();

        return new ListagemPaginada(data, PaginationInfo.of(pageSize, pageIndex * pageSize, page.getTotalElements()));
    }

    private Map<StatusSolicitacaoEnum, Long> carregarContagemPorStatus(UUID usuarioId) {
        Map<StatusSolicitacaoEnum, Long> contagem = SolicitacaoListagemResponseDTO.contagemVazia();
        for (Object[] row : solicitacaoRepository.countGroupedByStatusForCliente(usuarioId)) {
            contagem.put((StatusSolicitacaoEnum) row[0], (Long) row[1]);
        }
        return contagem;
    }

    private Map<UUID, Long> contarMatches(List<SolicitacaoEntity> solicitacoes) {
        if (solicitacoes.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = solicitacoes.stream().map(SolicitacaoEntity::getId).toList();
        Map<UUID, Long> resultado = new HashMap<>();
        for (Object[] row : solicitacaoMatchRepository.countGroupedBySolicitacaoIds(ids)) {
            resultado.put((UUID) row[0], (Long) row[1]);
        }
        return resultado;
    }

    private Map<String, String> carregarNomesEspecialidade(List<SolicitacaoEntity> solicitacoes) {
        Set<String> codigos = solicitacoes.stream()
                .map(SolicitacaoEntity::getEspecialidadeCodigo)
                .collect(Collectors.toSet());
        if (codigos.isEmpty()) {
            return Map.of();
        }
        return especialidadeRepository.findByCodigoIn(codigos).stream()
                .collect(Collectors.toMap(EspecialidadeEntity::getCodigo, EspecialidadeEntity::getNome));
    }

    private SolicitacaoMatchResponseDTO toMatchResponse(SolicitacaoMatchEntity match) {
        AdvogadoEntity advogado = match.getAdvogado();
        return SolicitacaoMatchResponseDTO.builder()
                .advogadoId(advogado.getUsuarioId())
                .nome(advogado.getNomeSocial() != null && !advogado.getNomeSocial().isBlank()
                        ? advogado.getNomeSocial()
                        : advogado.getNomeCompleto())
                .fotoUrl(advogado.getFotoUrl())
                .posicao(match.getPosicao())
                .compatibilidade(match.getScore())
                .nivelLocalidade(match.getNivelLocalidade())
                .mediaAvaliacoes(advogado.getMediaAvaliacoes())
                .totalAvaliacoes(advogado.getTotalAvaliacoes())
                .pontuacao(SolicitacaoMatchResponseDTO.PontuacaoMatchDTO.builder()
                        .modalidade(match.getPontosModalidade())
                        .localidade(match.getPontosLocalidade())
                        .especialidade(match.getPontosEspecialidade())
                        .subespecialidade(match.getPontosSubespecialidade())
                        .experiencia(match.getPontosExperiencia())
                        .formaCobranca(match.getPontosCobranca())
                        .build())
                .build();
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private SolicitacaoEntity obterSolicitacaoDoClienteAutenticado(UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));

        if (!solicitacao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return solicitacao;
    }

    private CriarSolicitacaoResponseDTO toResponse(SolicitacaoEntity entity, int totalMatches) {
        return CriarSolicitacaoResponseDTO.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .titulo(entity.getTitulo())
                .modalidade(entity.getModalidade())
                .especialidadeCodigo(entity.getEspecialidadeCodigo())
                .subespecialidadeCodigo(entity.getSubespecialidadeCodigo())
                .uf(entity.getUf())
                .cidade(entity.getCidade())
                .urgencia(entity.getUrgencia())
                .descricao(entity.getDescricao())
                .formaCobranca(entity.getFormaCobranca())
                .experienciaMinimaMeses(entity.getExperienciaMinimaMeses())
                .totalMatches(totalMatches)
                .criadoEm(entity.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
