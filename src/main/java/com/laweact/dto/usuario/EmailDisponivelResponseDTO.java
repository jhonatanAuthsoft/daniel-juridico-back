package com.laweact.dto.usuario;

import lombok.Builder;

@Builder
public record EmailDisponivelResponseDTO(
        boolean disponivel
) {}
