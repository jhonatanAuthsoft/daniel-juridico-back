package com.laweact.dto.solicitacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.laweact.dto.advogado.CatalogoItemResponseDTO;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
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
        /** Disponibilidade ATUAL do advogado, pode ter mudado após o matching. */
        DisponibilidadeAdvogadoEnum disponibilidade,
        BigDecimal mediaAvaliacoes,
        Integer totalAvaliacoes,
        String bairro,
        String cidade,
        CatalogoItemResponseDTO modalidadeAtuacao,
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
