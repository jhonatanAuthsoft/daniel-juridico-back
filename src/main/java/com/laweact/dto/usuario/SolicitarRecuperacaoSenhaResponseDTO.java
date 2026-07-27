package com.laweact.dto.usuario;

import lombok.Builder;

@Builder
public record SolicitarRecuperacaoSenhaResponseDTO(
        String mensagem,
        Integer aguardarSegundos
) {}
