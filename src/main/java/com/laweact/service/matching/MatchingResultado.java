package com.laweact.service.matching;

import com.laweact.model.enums.NivelLocalidadeEnum;

import lombok.Builder;

@Builder
public record MatchingResultado(
        boolean elegivel,
        int score,
        int pontosModalidade,
        int pontosLocalidade,
        int pontosEspecialidade,
        int pontosSubespecialidade,
        int pontosExperiencia,
        int pontosCobranca,
        NivelLocalidadeEnum nivelLocalidade
) {}
