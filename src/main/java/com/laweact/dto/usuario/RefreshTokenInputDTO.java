package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RefreshTokenInputDTO(
        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "O refreshToken é obrigatório")
        String refreshToken
) {}
