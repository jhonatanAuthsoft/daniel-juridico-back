package com.laweact.service.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.NivelLocalidadeEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("MatchingCalculator — score de compatibilidade")
class MatchingCalculatorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 3);

    private CriteriosMatching criteriosCompletos() {
        return CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.URGENTE)
                .formaCobranca(FormaCobrancaSolicitacaoEnum.VALOR_FIXO)
                .experienciaMinimaMeses(24)
                .build();
    }

    private AdvogadoSnapshot.AdvogadoSnapshotBuilder advogadoPerfeito() {
        return AdvogadoSnapshot.builder()
                .advogadoId(UUID.randomUUID())
                .nome("Ana Advogada")
                .disponivel(true)
                .modalidades(Set.of("CONSULTOR"))
                .especialidades(Set.of("CIVIL"))
                .subespecialidades(Set.of("CONTRATOS"))
                .areas(List.of(new AdvogadoSnapshot.Area("SP", "Sao Paulo")))
                .formasCobranca(Set.of("HONORARIOS_CONTRATUAIS"))
                .atuacaoDesde(LocalDate.of(2015, 1, 1))
                .mediaAvaliacoes(new BigDecimal("4.50"));
    }

    @Test
    @DisplayName("advogado 100% compatível na mesma cidade soma 100 pontos")
    void shouldScore100ForPerfectMatch() {
        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogadoPerfeito().build(), HOJE);

        assertThat(resultado.elegivel()).isTrue();
        assertThat(resultado.score()).isEqualTo(100);
        assertThat(resultado.nivelLocalidade()).isEqualTo(NivelLocalidadeEnum.MESMA_CIDADE);
        assertThat(resultado.pontosModalidade()).isEqualTo(20);
        assertThat(resultado.pontosLocalidade()).isEqualTo(20);
        assertThat(resultado.pontosEspecialidade()).isEqualTo(20);
        assertThat(resultado.pontosSubespecialidade()).isEqualTo(20);
        assertThat(resultado.pontosExperiencia()).isEqualTo(10);
        assertThat(resultado.pontosCobranca()).isEqualTo(10);
    }

    @Test
    @DisplayName("mesma UF em outra cidade pontua 10 na localidade")
    void shouldScore10ForSameState() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .areas(List.of(new AdvogadoSnapshot.Area("SP", "Campinas")))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.elegivel()).isTrue();
        assertThat(resultado.score()).isEqualTo(90);
        assertThat(resultado.nivelLocalidade()).isEqualTo(NivelLocalidadeEnum.MESMO_ESTADO);
    }

    @Test
    @DisplayName("fora do estado não entra no ranking")
    void shouldRejectOutOfState() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .areas(List.of(new AdvogadoSnapshot.Area("RJ", "Rio de Janeiro")))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.elegivel()).isFalse();
        assertThat(resultado.nivelLocalidade()).isEqualTo(NivelLocalidadeEnum.FORA_ESTADO);
    }

    @Test
    @DisplayName("advogado indisponível não entra no ranking")
    void shouldRejectUnavailable() {
        MatchingResultado resultado = MatchingCalculator.avaliar(
                criteriosCompletos(),
                advogadoPerfeito().disponivel(false).build(),
                HOJE
        );

        assertThat(resultado.elegivel()).isFalse();
    }

    @Test
    @DisplayName("modalidade incompatível não entra no ranking")
    void shouldRejectIncompatibleModalidade() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .modalidades(Set.of("PAUTISTA"))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.elegivel()).isFalse();
    }

    @Test
    @DisplayName("PROCESSO aceita pautista, generalista e correspondente")
    void shouldMatchProcessoModalidades() {
        CriteriosMatching criterios = CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.PROCESSO)
                .especialidadeCodigo("CIVIL")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .build();

        for (String modalidade : List.of("PAUTISTA", "GENERALISTA", "CORRESPONDENTE")) {
            MatchingResultado resultado = MatchingCalculator.avaliar(
                    criterios,
                    advogadoPerfeito().modalidades(Set.of(modalidade)).build(),
                    HOJE
            );
            assertThat(resultado.pontosModalidade())
                    .as("modalidade %s deveria pontuar", modalidade)
                    .isEqualTo(20);
        }
    }

    @Test
    @DisplayName("'nenhuma das anteriores' entra pela especialidade compatível com 0 em modalidade")
    void shouldAllowNenhumaDasAnterioresWithMatchingSpecialty() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .modalidades(Set.of("NENHUMA_DAS_ANTERIORES"))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.elegivel()).isTrue();
        assertThat(resultado.pontosModalidade()).isZero();
        assertThat(resultado.pontosEspecialidade()).isEqualTo(20);
        assertThat(resultado.score()).isEqualTo(80);
    }

    @Test
    @DisplayName("'nenhuma das anteriores' com especialidade incompatível fica fora")
    void shouldRejectNenhumaDasAnterioresWithoutSpecialty() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .modalidades(Set.of("NENHUMA_DAS_ANTERIORES"))
                .especialidades(Set.of("TRIBUTARIO"))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.elegivel()).isFalse();
    }

    @Test
    @DisplayName("modalidade ampla garante especialidade e subespecialidade mesmo sem cadastro")
    void shouldGrantSpecialtyPointsForBroadModalidade() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .modalidades(Set.of("GENERALISTA"))
                .especialidades(Set.of())
                .subespecialidades(Set.of())
                .build();

        CriteriosMatching criterios = CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.MEDIACAO)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criterios, advogado, HOJE);

        assertThat(resultado.pontosEspecialidade()).isEqualTo(20);
        assertThat(resultado.pontosSubespecialidade()).isEqualTo(20);
        assertThat(resultado.score()).isEqualTo(100);
    }

    @Test
    @DisplayName("critérios opcionais não informados pontuam integralmente")
    void shouldGrantFullPointsForOmittedOptionalCriteria() {
        CriteriosMatching criterios = CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.TENHO_TEMPO)
                .build();

        AdvogadoSnapshot advogado = advogadoPerfeito()
                .subespecialidades(Set.of())
                .formasCobranca(Set.of())
                .atuacaoDesde(LocalDate.of(2026, 7, 1))
                .modalidades(Set.of("NENHUMA_DAS_ANTERIORES"))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criterios, advogado, HOJE);

        assertThat(resultado.pontosSubespecialidade()).isEqualTo(20);
        assertThat(resultado.pontosCobranca()).isEqualTo(10);
        assertThat(resultado.pontosExperiencia()).isEqualTo(10);
    }

    @Test
    @DisplayName("experiência abaixo do mínimo zera os 10 pontos")
    void shouldZeroExperienceWhenBelowMinimum() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .atuacaoDesde(LocalDate.of(2026, 1, 1))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.pontosExperiencia()).isZero();
        assertThat(resultado.score()).isEqualTo(90);
    }

    @Test
    @DisplayName("forma de cobrança incompatível zera os 10 pontos")
    void shouldZeroBillingWhenIncompatible() {
        AdvogadoSnapshot advogado = advogadoPerfeito()
                .formasCobranca(Set.of("HONORARIOS_PERCENTUAIS"))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criteriosCompletos(), advogado, HOJE);

        assertThat(resultado.pontosCobranca()).isZero();
        assertThat(resultado.score()).isEqualTo(90);
    }

    @Test
    @DisplayName("cliente que escolhe negociar aceita qualquer forma de cobrança")
    void shouldAcceptAnyBillingWhenNegociar() {
        CriteriosMatching criterios = CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .formaCobranca(FormaCobrancaSolicitacaoEnum.NEGOCIAR)
                .build();

        AdvogadoSnapshot advogado = advogadoPerfeito()
                .formasCobranca(Set.of("HONORARIOS_ARBITRADOS"))
                .build();

        assertThat(MatchingCalculator.avaliar(criterios, advogado, HOJE).pontosCobranca()).isEqualTo(10);
    }

    @Test
    @DisplayName("escolhe a modalidade compatível mais específica para exibir no card")
    void shouldPickMostSpecificCompatibleModalityForCard() {
        assertThat(MatchingCalculator.escolherCodigoModalidade(
                ModalidadeSolicitacaoEnum.CONSULTORIA,
                Set.of("PAUTISTA", "CONSULTOR", "GENERALISTA")
        )).isEqualTo("CONSULTOR");

        assertThat(MatchingCalculator.escolherCodigoModalidade(
                ModalidadeSolicitacaoEnum.PROCESSO,
                Set.of("GENERALISTA", "PAUTISTA")
        )).isEqualTo("PAUTISTA");

        assertThat(MatchingCalculator.escolherCodigoModalidade(
                ModalidadeSolicitacaoEnum.CONSULTORIA,
                Set.of("NENHUMA_DAS_ANTERIORES")
        )).isEqualTo("NENHUMA_DAS_ANTERIORES");
    }

    @Test
    @DisplayName("score abaixo de 40 fica fora da listagem")
    void shouldRejectScoreBelowCut() {
        CriteriosMatching criterios = CriteriosMatching.builder()
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .formaCobranca(FormaCobrancaSolicitacaoEnum.VALOR_FIXO)
                .experienciaMinimaMeses(240)
                .build();

        AdvogadoSnapshot advogado = advogadoPerfeito()
                .modalidades(Set.of("NENHUMA_DAS_ANTERIORES"))
                .subespecialidades(Set.of("OUTRA"))
                .formasCobranca(Set.of("HONORARIOS_PERCENTUAIS"))
                .atuacaoDesde(LocalDate.of(2024, 1, 1))
                .areas(List.of(new AdvogadoSnapshot.Area("SP", "Campinas")))
                .build();

        MatchingResultado resultado = MatchingCalculator.avaliar(criterios, advogado, HOJE);

        assertThat(resultado.score()).isEqualTo(30);
        assertThat(resultado.elegivel()).isFalse();
    }
}
