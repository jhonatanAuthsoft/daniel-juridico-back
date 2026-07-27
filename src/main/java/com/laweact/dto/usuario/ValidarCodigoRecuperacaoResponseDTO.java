package com.laweact.dto.usuario;

import lombok.Builder;

@Builder
public record ValidarCodigoRecuperacaoResponseDTO(
        boolean valido,
        String mensagem
) {}
