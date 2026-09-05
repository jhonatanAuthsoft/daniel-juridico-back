package com.laweact.service.matching;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.NivelLocalidadeEnum;

/**
 * Score de compatibilidade (0–100) conforme escopo "Algoritmo de Matching e Ranking".
 * Sem dependência de banco para permitir teste unitário direto.
 */
public final class MatchingCalculator {

    public static final int SCORE_MINIMO = 40;

    private static final int PESO_MODALIDADE = 20;
    private static final int PESO_LOCALIDADE_CIDADE = 20;
    private static final int PESO_LOCALIDADE_ESTADO = 10;
    private static final int PESO_ESPECIALIDADE = 20;
    private static final int PESO_SUBESPECIALIDADE = 20;
    private static final int PESO_EXPERIENCIA = 10;
    private static final int PESO_COBRANCA = 10;

    private static final String MODALIDADE_NENHUMA = "NENHUMA_DAS_ANTERIORES";

    /** Modalidades do advogado que atendem qualquer especialidade (não selecionam especialidades no cadastro). */
    private static final Set<String> MODALIDADES_AMPLAS =
            Set.of("PAUTISTA", "GENERALISTA", "CONSULTOR", "CORRESPONDENTE");

    private static final Map<ModalidadeSolicitacaoEnum, Set<String>> MODALIDADES_COMPATIVEIS = Map.of(
            ModalidadeSolicitacaoEnum.CONSULTORIA, Set.of("CONSULTOR", "GENERALISTA"),
            ModalidadeSolicitacaoEnum.PROCESSO, Set.of("PAUTISTA", "GENERALISTA", "CORRESPONDENTE"),
            ModalidadeSolicitacaoEnum.MEDIACAO, Set.of("CONSULTOR", "GENERALISTA")
    );

    /** Ordem de preferência no card: mais específica antes de ampla. */
    private static final List<String> ORDEM_EXIBICAO_MODALIDADE = List.of(
            "PAUTISTA",
            "CONSULTOR",
            "CORRESPONDENTE",
            "GENERALISTA",
            MODALIDADE_NENHUMA
    );

    private static final Map<FormaCobrancaSolicitacaoEnum, Set<String>> COBRANCAS_COMPATIVEIS = Map.of(
            FormaCobrancaSolicitacaoEnum.VALOR_FIXO, Set.of("HONORARIOS_CONTRATUAIS"),
            FormaCobrancaSolicitacaoEnum.HORA, Set.of("HONORARIOS_CONTRATUAIS"),
            FormaCobrancaSolicitacaoEnum.EXITO, Set.of("HONORARIOS_PERCENTUAIS"),
            // "A combinar" indica cliente flexível: qualquer forma do advogado serve.
            FormaCobrancaSolicitacaoEnum.NEGOCIAR, Set.of(
                    "HONORARIOS_CONTRATUAIS",
                    "HONORARIOS_PERCENTUAIS",
                    "HONORARIOS_ARBITRADOS",
                    "OUTROS_A_COMBINAR"
            )
    );

    private MatchingCalculator() {
    }

    public static MatchingResultado avaliar(CriteriosMatching criterios, AdvogadoSnapshot advogado, LocalDate hoje) {
        NivelLocalidadeEnum nivelLocalidade = resolverNivelLocalidade(criterios, advogado);

        boolean modalidadeCompativel = temInterseccao(
                advogado.modalidades(),
                MODALIDADES_COMPATIVEIS.getOrDefault(criterios.modalidade(), Set.of())
        );
        boolean modalidadeAmpla = temInterseccao(advogado.modalidades(), MODALIDADES_AMPLAS);
        boolean declarouNenhuma = contem(advogado.modalidades(), MODALIDADE_NENHUMA);

        int pontosEspecialidade = modalidadeAmpla || contem(advogado.especialidades(), criterios.especialidadeCodigo())
                ? PESO_ESPECIALIDADE
                : 0;

        int pontosModalidade = modalidadeCompativel ? PESO_MODALIDADE : 0;
        int pontosLocalidade = switch (nivelLocalidade) {
            case MESMA_CIDADE -> PESO_LOCALIDADE_CIDADE;
            case MESMO_ESTADO -> PESO_LOCALIDADE_ESTADO;
            case FORA_ESTADO -> 0;
        };
        int pontosSubespecialidade = pontuarSubespecialidade(criterios, advogado, modalidadeAmpla);
        int pontosExperiencia = pontuarExperiencia(criterios, advogado, hoje);
        int pontosCobranca = pontuarCobranca(criterios, advogado);

        int score = pontosModalidade
                + pontosLocalidade
                + pontosEspecialidade
                + pontosSubespecialidade
                + pontosExperiencia
                + pontosCobranca;

        // Advogado sem modalidade exigida só concorre quando declarou "nenhuma das anteriores"
        // e a especialidade cadastrada atende a demanda.
        boolean atendeModalidade = modalidadeCompativel
                || (declarouNenhuma && pontosEspecialidade > 0);
        boolean elegivel = advogado.disponivel()
                && nivelLocalidade != NivelLocalidadeEnum.FORA_ESTADO
                && atendeModalidade
                && score >= SCORE_MINIMO;

        return MatchingResultado.builder()
                .elegivel(elegivel)
                .score(score)
                .pontosModalidade(pontosModalidade)
                .pontosLocalidade(pontosLocalidade)
                .pontosEspecialidade(pontosEspecialidade)
                .pontosSubespecialidade(pontosSubespecialidade)
                .pontosExperiencia(pontosExperiencia)
                .pontosCobranca(pontosCobranca)
                .nivelLocalidade(nivelLocalidade)
                .build();
    }

    private static NivelLocalidadeEnum resolverNivelLocalidade(CriteriosMatching criterios, AdvogadoSnapshot advogado) {
        List<AdvogadoSnapshot.Area> areas = advogado.areas() == null ? List.of() : advogado.areas();
        String ufDemanda = normalizar(criterios.uf());
        String cidadeDemanda = normalizar(criterios.cidade());

        boolean mesmoEstado = false;
        for (AdvogadoSnapshot.Area area : areas) {
            if (!ufDemanda.equals(normalizar(area.uf()))) {
                continue;
            }
            mesmoEstado = true;
            if (cidadeDemanda.equals(normalizar(area.cidade()))) {
                return NivelLocalidadeEnum.MESMA_CIDADE;
            }
        }
        return mesmoEstado ? NivelLocalidadeEnum.MESMO_ESTADO : NivelLocalidadeEnum.FORA_ESTADO;
    }

    /**
     * Modalidade do advogado a exibir no card de compatíveis: a mais específica
     * que atende a solicitação; se nenhuma for compatível, a primeira do cadastro.
     */
    public static String escolherCodigoModalidade(
            ModalidadeSolicitacaoEnum modalidadeSolicitacao,
            Collection<String> modalidadesAdvogado
    ) {
        if (modalidadesAdvogado == null || modalidadesAdvogado.isEmpty()) {
            return null;
        }
        Set<String> doAdvogado = new HashSet<>();
        for (String codigo : modalidadesAdvogado) {
            if (!isBlank(codigo)) {
                doAdvogado.add(normalizar(codigo));
            }
        }
        if (doAdvogado.isEmpty()) {
            return null;
        }
        Set<String> compativeis = MODALIDADES_COMPATIVEIS.getOrDefault(modalidadeSolicitacao, Set.of());
        for (String codigo : ORDEM_EXIBICAO_MODALIDADE) {
            if (doAdvogado.contains(codigo) && compativeis.contains(codigo)) {
                return codigo;
            }
        }
        for (String codigo : ORDEM_EXIBICAO_MODALIDADE) {
            if (doAdvogado.contains(codigo)) {
                return codigo;
            }
        }
        return doAdvogado.iterator().next();
    }

    private static int pontuarSubespecialidade(
            CriteriosMatching criterios,
            AdvogadoSnapshot advogado,
            boolean modalidadeAmpla
    ) {
        if (isBlank(criterios.subespecialidadeCodigo()) || modalidadeAmpla) {
            return PESO_SUBESPECIALIDADE;
        }
        return contem(advogado.subespecialidades(), criterios.subespecialidadeCodigo()) ? PESO_SUBESPECIALIDADE : 0;
    }

    private static int pontuarExperiencia(CriteriosMatching criterios, AdvogadoSnapshot advogado, LocalDate hoje) {
        Integer minimo = criterios.experienciaMinimaMeses();
        if (minimo == null || minimo <= 0) {
            return PESO_EXPERIENCIA;
        }
        if (advogado.atuacaoDesde() == null) {
            return 0;
        }
        long meses = ChronoUnit.MONTHS.between(advogado.atuacaoDesde(), hoje);
        return meses >= minimo ? PESO_EXPERIENCIA : 0;
    }

    private static int pontuarCobranca(CriteriosMatching criterios, AdvogadoSnapshot advogado) {
        FormaCobrancaSolicitacaoEnum desejada = criterios.formaCobranca();
        if (desejada == null) {
            return PESO_COBRANCA;
        }
        return temInterseccao(advogado.formasCobranca(), COBRANCAS_COMPATIVEIS.getOrDefault(desejada, Set.of()))
                ? PESO_COBRANCA
                : 0;
    }

    private static boolean temInterseccao(Collection<String> valores, Set<String> esperados) {
        if (valores == null || valores.isEmpty() || esperados.isEmpty()) {
            return false;
        }
        for (String valor : valores) {
            if (esperados.contains(normalizar(valor))) {
                return true;
            }
        }
        return false;
    }

    private static boolean contem(Collection<String> valores, String esperado) {
        if (valores == null || isBlank(esperado)) {
            return false;
        }
        String alvo = normalizar(esperado);
        for (String valor : valores) {
            if (alvo.equals(normalizar(valor))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
