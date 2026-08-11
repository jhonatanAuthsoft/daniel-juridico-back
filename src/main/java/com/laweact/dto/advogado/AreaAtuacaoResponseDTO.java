package com.laweact.dto.advogado;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AreaAtuacaoResponseDTO(
        @NotNull UUID id,
        @NotBlank String estado,
        @NotBlank String cidade
) {}
