package com.laweact.dto.usuario;

import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EditarUsuarioInputDTO(
        @NotBlank(message = "O nome completo é obrigatório") String nomeCompleto,
        @NotBlank(message = "O e-mail é obrigatório") @Email(message = "O e-mail deve ser válido") String email,
        String senha, // Opcional, atualiza apenas se for enviada
        @NotNull(message = "O perfil é obrigatório") PerfilUsuarioEnum perfil,
        @NotNull(message = "O status é obrigatório") StatusUsuarioEnum status,
        String telefone
) {}
