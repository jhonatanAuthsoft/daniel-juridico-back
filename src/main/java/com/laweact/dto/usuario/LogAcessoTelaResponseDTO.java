package com.laweact.dto.usuario;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.TelaAcessoEnum;

import lombok.Builder;

@Builder
public record LogAcessoTelaResponseDTO(
        UUID id,
        UUID usuarioId,
        TelaAcessoEnum tela,
        LocalDateTime acessadoEm
) {}
