package com.laweact.dto.usuario;

import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CadastrarUsuarioInputDTO(
        @NotBlank(message = "O nome completo é obrigatório") String nomeCompleto,
        @NotBlank(message = "O e-mail é obrigatório") @Email(message = "O e-mail deve ser válido") String email,
        @NotBlank(message = "A senha é obrigatória") String senha,
        @NotNull(message = "O perfil é obrigatório") PerfilUsuarioEnum perfil,
        StatusUsuarioEnum status,
        String telefone
) {}
