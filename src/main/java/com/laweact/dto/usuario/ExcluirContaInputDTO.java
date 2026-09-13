package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ExcluirContaInputDTO(
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}
