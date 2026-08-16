package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PreferenciasResponseDTO(
        @NotNull Boolean notificacoesPushHabilitadas
) {}
