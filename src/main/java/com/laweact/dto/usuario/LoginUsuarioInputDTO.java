package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record LoginUsuarioInputDTO(
        @NotBlank(message = "O e-mail é obrigatório") @NotNull String email,
        @NotBlank(message = "A senha é obrigatória") @NotNull String senha,
        String deviceId
) {}
