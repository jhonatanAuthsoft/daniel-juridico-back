package com.laweact.dto.usuario;

import lombok.Builder;

@Builder
public record AtualizarSenhaResponseDTO(
        String mensagem
) {}
