package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AtualizarFotoInputDTO(
        @NotBlank(message = "A foto de perfil é obrigatória")
        String fotoUrl
) {}
