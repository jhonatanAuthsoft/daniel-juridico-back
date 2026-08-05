package com.laweact.dto.solicitacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.laweact.model.enums.NivelLocalidadeEnum;

import lombok.Builder;

@Builder
public record SolicitacaoMatchResponseDTO(
        UUID advogadoId,
        String nome,
        String fotoUrl,
        Integer posicao,
        /** Percentual de compatibilidade (0–100) exibido no card. */
        Integer compatibilidade,
        NivelLocalidadeEnum nivelLocalidade,
        BigDecimal mediaAvaliacoes,
        Integer totalAvaliacoes,
        PontuacaoMatchDTO pontuacao
) {

    @Builder
    public record PontuacaoMatchDTO(
            Integer modalidade,
            Integer localidade,
            Integer especialidade,
            Integer subespecialidade,
            Integer experiencia,
            Integer formaCobranca
    ) {}
}
