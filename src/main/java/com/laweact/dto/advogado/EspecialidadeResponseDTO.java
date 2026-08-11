package com.laweact.dto.advogado;

import java.util.UUID;

import lombok.Builder;

@Builder
public record EspecialidadeResponseDTO(
        UUID id,
        String especialidadeCodigo,
        String especialidadeNome,
        String especialidadeLivre,
        String subespecialidadeCodigo,
        String subespecialidadeNome,
        String subespecialidadeLivre
) {}
