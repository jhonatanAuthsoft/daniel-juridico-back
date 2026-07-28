package com.laweact.dto.cliente;

import com.laweact.dto.usuario.UsuarioResponseDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CadastrarClienteResponseDTO(
        @NotNull UsuarioResponseDTO usuario,
        @NotNull ClientePerfilResponseDTO cliente,
        @NotNull EnderecoResponseDTO endereco,
        @NotBlank String token,
        @NotBlank String refreshToken
) {}
