package com.laweact.dto.usuario;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record LoginUsuarioResponseDTO(
        @Valid @NotNull UsuarioResponseDTO usuario,
        ClienteDetalheResponseDTO cliente,
        AdvogadoDetalheResponseDTO advogado,
        @NotNull @NotBlank String token,
        @NotNull @NotBlank String refreshToken
) {}
