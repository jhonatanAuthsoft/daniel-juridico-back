package com.laweact.dto.catalogo;

import java.util.List;

import lombok.Builder;

@Builder
public record EspecialidadeCatalogoItemDTO(
        String codigo,
        String nome,
        List<SubespecialidadeCatalogoItemDTO> subespecialidades
) {}
