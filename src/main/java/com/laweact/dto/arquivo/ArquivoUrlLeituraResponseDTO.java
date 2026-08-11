package com.laweact.dto.arquivo;

import lombok.Builder;

@Builder
public record ArquivoUrlLeituraResponseDTO(
        String key,
        String readUrl,
        long expiresInSeconds
) {}
