package com.laweact.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SolicitarRecuperacaoSenhaInputDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email
) {}
