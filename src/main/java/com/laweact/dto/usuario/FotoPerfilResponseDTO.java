package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record FotoPerfilResponseDTO(
        @NotBlank String fotoUrl
) {}
