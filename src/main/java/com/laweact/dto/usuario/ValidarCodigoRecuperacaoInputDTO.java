package com.laweact.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record ValidarCodigoRecuperacaoInputDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "^\\d{4}$", message = "O código deve conter 4 dígitos")
        String codigo
) {}
