package com.laweact.dto.avaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AvaliacaoItemResponseDTO(
        UUID id,
        BigDecimal nota,
        String comentario,
        String nomeAvaliador,
        LocalDateTime criadoEm,
        boolean propria
) {}
