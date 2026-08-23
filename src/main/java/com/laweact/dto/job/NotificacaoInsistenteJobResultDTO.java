package com.laweact.dto.job;

import lombok.Builder;

@Builder
public record NotificacaoInsistenteJobResultDTO(
        int avaliadas,
        int reenviadas,
        int ignoradas,
        int erros
) {}
