package com.laweact.dto.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record LoginUsuarioResponseDTO(
        @Valid UsuarioResponseDTO usuario,
        @NotNull @NotBlank String token
) {}
