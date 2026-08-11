package com.laweact.service.matching;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;

/** Retrato do advogado carregado em lote para o cálculo do ranking. */
@Builder
public record AdvogadoSnapshot(
        UUID advogadoId,
        String nome,
        boolean disponivel,
        Set<String> modalidades,
        Set<String> especialidades,
        Set<String> subespecialidades,
        List<Area> areas,
        Set<String> formasCobranca,
        LocalDate atuacaoDesde,
        BigDecimal mediaAvaliacoes
) {

    public record Area(String uf, String cidade) {}
}
