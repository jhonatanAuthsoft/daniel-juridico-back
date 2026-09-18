package com.laweact.dto.advogado;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AtualizarEspecialidadesAdvogadoInputDTO(
        @NotNull(message = "Informe as especialidades")
        @Valid
        List<EspecialidadeInputDTO> especialidades
) {}
