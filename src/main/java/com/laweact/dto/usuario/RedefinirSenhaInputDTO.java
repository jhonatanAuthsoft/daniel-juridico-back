package com.laweact.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RedefinirSenhaInputDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        String email,

        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "^\\d{4}$", message = "O código deve conter 4 dígitos")
        String codigo,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos 1 letra maiúscula, 1 minúscula e 1 número"
        )
        String novaSenha,

        @NotBlank(message = "A confirmação de senha é obrigatória")
        String confirmarSenha
) {}
