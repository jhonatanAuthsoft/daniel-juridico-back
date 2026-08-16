package com.laweact.dto.usuario;

import java.util.UUID;

import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UsuarioResponseDTO(
        @NotNull UUID id,
        @NotNull @NotBlank String nomeCompleto,
        @NotNull @NotBlank String email,
        @NotNull StatusUsuarioEnum status,
        @NotNull PerfilUsuarioEnum perfil,
        String telefone,
        @NotNull Boolean termosAceitos,
        @NotNull Boolean notificacoesPushHabilitadas
) {}
