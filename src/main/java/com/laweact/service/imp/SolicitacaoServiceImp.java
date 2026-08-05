package com.laweact.service.imp;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SolicitacaoServiceImp implements SolicitacaoService {

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
    public List<SolicitacaoMatchResponseDTO> listarMatches(UUID solicitacaoId) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        SolicitacaoEntity solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new CustomError("Solicitação não encontrada", HttpStatus.NOT_FOUND));

        if (!solicitacao.getCliente().getUsuarioId().equals(usuario.getId())) {
            throw new CustomError("Solicitação de outro cliente", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        return solicitacaoMatchRepository.findRankingBySolicitacaoId(solicitacaoId)
                .stream()
                .map(this::toMatchResponse)
                .toList();
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
