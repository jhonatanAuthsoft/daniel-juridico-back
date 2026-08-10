package com.laweact.dto.avaliacao;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;

@Builder
public record AvaliacaoListagemResponseDTO(
        List<AvaliacaoItemResponseDTO> items,
        BigDecimal mediaAvaliacoes,
        long totalAvaliacoes,
        boolean podeAvaliar
) {}
