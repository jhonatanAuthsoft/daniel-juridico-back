package com.laweact.dto.notificacao;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record NaoLidasExisteResponseDTO(
        @NotNull Boolean existe
) {}
