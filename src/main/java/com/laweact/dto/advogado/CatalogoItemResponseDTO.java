package com.laweact.dto.advogado;

import lombok.Builder;

@Builder
public record CatalogoItemResponseDTO(
        String codigo,
        String nome
) {}
