package com.laweact.service.imp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.SolicitacaoMatchEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.repository.AdvogadoEspecialidadeRepository;
import com.laweact.repository.AdvogadoFormaCobrancaRepository;
import com.laweact.repository.AdvogadoModalidadeRepository;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.AreaAtuacaoAdvogadoRepository;
import com.laweact.repository.SolicitacaoMatchRepository;
import com.laweact.service.MatchingService;
import com.laweact.service.matching.AdvogadoSnapshot;
import com.laweact.service.matching.CriteriosMatching;
import com.laweact.service.matching.MatchingCalculator;
import com.laweact.service.matching.MatchingResultado;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MatchingServiceImp implements MatchingService {

    private final AdvogadoRepository advogadoRepository;
    private final AdvogadoModalidadeRepository advogadoModalidadeRepository;
    private final AdvogadoEspecialidadeRepository advogadoEspecialidadeRepository;
    private final AdvogadoFormaCobrancaRepository advogadoFormaCobrancaRepository;
    private final AreaAtuacaoAdvogadoRepository areaAtuacaoAdvogadoRepository;
    private final SolicitacaoMatchRepository solicitacaoMatchRepository;

    @Override
    @Transactional
    public List<SolicitacaoMatchEntity> gerarMatches(SolicitacaoEntity solicitacao) {
        List<AdvogadoEntity> candidatos = advogadoRepository.findCandidatosMatching(
                DisponibilidadeAdvogadoEnum.DISPONIVEL,
                StatusUsuarioEnum.ATIVO
        );
        if (candidatos.isEmpty()) {
            return List.of();
        }

        List<AdvogadoSnapshot> snapshots = carregarSnapshots(candidatos);
        CriteriosMatching criterios = toCriterios(solicitacao);
        LocalDate hoje = LocalDate.now();

        List<Avaliado> elegiveis = new ArrayList<>();
        for (AdvogadoSnapshot snapshot : snapshots) {
            MatchingResultado resultado = MatchingCalculator.avaliar(criterios, snapshot, hoje);
            if (resultado.elegivel()) {
                elegiveis.add(new Avaliado(snapshot, resultado));
            }
        }

        elegiveis.sort(ranking());

        Map<UUID, AdvogadoEntity> porId = new HashMap<>();
        candidatos.forEach(advogado -> porId.put(advogado.getUsuarioId(), advogado));

        List<SolicitacaoMatchEntity> matches = new ArrayList<>();
        int posicao = 1;
        for (Avaliado avaliado : elegiveis) {
            MatchingResultado resultado = avaliado.resultado();
            matches.add(SolicitacaoMatchEntity.builder()
                    .solicitacao(solicitacao)
                    .advogado(porId.get(avaliado.snapshot().advogadoId()))
                    .posicao(posicao++)
                    .score(resultado.score())
                    .nivelLocalidade(resultado.nivelLocalidade())
                    .pontosModalidade(resultado.pontosModalidade())
                    .pontosLocalidade(resultado.pontosLocalidade())
                    .pontosEspecialidade(resultado.pontosEspecialidade())
                    .pontosSubespecialidade(resultado.pontosSubespecialidade())
                    .pontosExperiencia(resultado.pontosExperiencia())
                    .pontosCobranca(resultado.pontosCobranca())
                    .build());
        }

        List<SolicitacaoMatchEntity> salvos = solicitacaoMatchRepository.saveAll(matches);
        log.info(
                "Matching da solicitação {}: {} advogados avaliados, {} compatíveis",
                solicitacao.getId(),
                snapshots.size(),
                salvos.size()
        );
        return salvos;
    }

    private Comparator<Avaliado> ranking() {
        return Comparator
                .comparingInt((Avaliado a) -> a.resultado().score()).reversed()
                // MESMA_CIDADE vem antes de MESMO_ESTADO (ordinal crescente)
                .thenComparingInt(a -> a.resultado().nivelLocalidade().ordinal())
                .thenComparing(
                        a -> a.snapshot().mediaAvaliacoes() == null
                                ? BigDecimal.ZERO
                                : a.snapshot().mediaAvaliacoes(),
                        Comparator.reverseOrder()
                )
                .thenComparing(a -> a.snapshot().nome() == null ? "" : a.snapshot().nome(),
                        String.CASE_INSENSITIVE_ORDER);
    }

    private CriteriosMatching toCriterios(SolicitacaoEntity solicitacao) {
        return CriteriosMatching.builder()
                .modalidade(solicitacao.getModalidade())
                .especialidadeCodigo(solicitacao.getEspecialidadeCodigo())
                .subespecialidadeCodigo(solicitacao.getSubespecialidadeCodigo())
                .uf(solicitacao.getUf())
                .cidade(solicitacao.getCidade())
                .urgencia(solicitacao.getUrgencia())
                .formaCobranca(solicitacao.getFormaCobranca())
                .experienciaMinimaMeses(solicitacao.getExperienciaMinimaMeses())
                .build();
    }

    private List<AdvogadoSnapshot> carregarSnapshots(List<AdvogadoEntity> candidatos) {
        List<UUID> ids = candidatos.stream().map(AdvogadoEntity::getUsuarioId).toList();

        Map<UUID, Set<String>> modalidades = agruparCodigos(
                advogadoModalidadeRepository.findCodigosByAdvogadoIds(ids)
        );
        Map<UUID, Set<String>> formasCobranca = agruparCodigos(
                advogadoFormaCobrancaRepository.findCodigosByAdvogadoIds(ids)
        );

        Map<UUID, Set<String>> especialidades = new HashMap<>();
        Map<UUID, Set<String>> subespecialidades = new HashMap<>();
        for (Object[] linha : advogadoEspecialidadeRepository.findCodigosByAdvogadoIds(ids)) {
            UUID advogadoId = (UUID) linha[0];
            if (linha[1] != null) {
                especialidades.computeIfAbsent(advogadoId, k -> new HashSet<>()).add((String) linha[1]);
            }
            if (linha[2] != null) {
                subespecialidades.computeIfAbsent(advogadoId, k -> new HashSet<>()).add((String) linha[2]);
            }
        }

        Map<UUID, List<AdvogadoSnapshot.Area>> areas = new HashMap<>();
        for (Object[] linha : areaAtuacaoAdvogadoRepository.findAreasByAdvogadoIds(ids)) {
            UUID advogadoId = (UUID) linha[0];
            areas.computeIfAbsent(advogadoId, k -> new ArrayList<>())
                    .add(new AdvogadoSnapshot.Area((String) linha[1], (String) linha[2]));
        }

        List<AdvogadoSnapshot> snapshots = new ArrayList<>();
        for (AdvogadoEntity advogado : candidatos) {
            UUID id = advogado.getUsuarioId();
            snapshots.add(AdvogadoSnapshot.builder()
                    .advogadoId(id)
                    .nome(advogado.getNomeCompleto())
                    .disponivel(advogado.getDisponibilidade() == DisponibilidadeAdvogadoEnum.DISPONIVEL)
                    .modalidades(modalidades.getOrDefault(id, Set.of()))
                    .especialidades(especialidades.getOrDefault(id, Set.of()))
                    .subespecialidades(subespecialidades.getOrDefault(id, Set.of()))
                    .areas(areas.getOrDefault(id, List.of()))
                    .formasCobranca(formasCobranca.getOrDefault(id, Set.of()))
                    .atuacaoDesde(advogado.getAtuacaoDesde())
                    .mediaAvaliacoes(advogado.getMediaAvaliacoes())
                    .build());
        }
        return snapshots;
    }

    private Map<UUID, Set<String>> agruparCodigos(List<Object[]> linhas) {
        Map<UUID, Set<String>> agrupado = new HashMap<>();
        for (Object[] linha : linhas) {
            agrupado.computeIfAbsent((UUID) linha[0], k -> new HashSet<>()).add((String) linha[1]);
        }
        return agrupado;
    }

    private record Avaliado(AdvogadoSnapshot snapshot, MatchingResultado resultado) {}
}
