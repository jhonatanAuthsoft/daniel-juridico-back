package com.laweact.dto.catalogo;

import lombok.Builder;

@Builder
public record SubespecialidadeCatalogoItemDTO(
        String codigo,
        String nome
) {}
