package com.laweact.dto.arquivo;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ArquivoUrlLeituraInputDTO(
        @NotBlank(message = "A key é obrigatória")
        String key
) {}
