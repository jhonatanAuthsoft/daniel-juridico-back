package com.laweact.dto.advogado;

import lombok.Builder;

@Builder
public record EspecialidadeInputDTO(
        String especialidadeCodigo,
        String especialidadeLivre,
        String subespecialidadeCodigo,
        String subespecialidadeLivre
) {}
