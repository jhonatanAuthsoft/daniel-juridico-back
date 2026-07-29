package com.laweact.dto.arquivo;

import java.util.Map;

import lombok.Builder;

@Builder
public record ArquivoUrlUploadResponseDTO(
        String key,
        String uploadUrl,
        long expiresInSeconds,
        Map<String, String> requiredHeaders
) {}
