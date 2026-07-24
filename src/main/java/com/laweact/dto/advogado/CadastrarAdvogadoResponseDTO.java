package com.laweact.dto.advogado;

import java.util.List;

import com.laweact.dto.cliente.EnderecoResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CadastrarAdvogadoResponseDTO(
        @NotNull UsuarioResponseDTO usuario,
        @NotNull AdvogadoPerfilResponseDTO advogado,
        @NotNull EnderecoResponseDTO endereco,
        @NotNull List<OabResponseDTO> oabs,
        @NotNull List<AreaAtuacaoResponseDTO> areasAtuacao,
        @NotBlank String token
) {}
