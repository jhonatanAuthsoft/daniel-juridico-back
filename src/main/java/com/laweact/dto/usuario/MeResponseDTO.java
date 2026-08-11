package com.laweact.dto.usuario;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MeResponseDTO(
        @Valid @NotNull UsuarioResponseDTO usuario,
        ClienteDetalheResponseDTO cliente,
        AdvogadoDetalheResponseDTO advogado
) {}
