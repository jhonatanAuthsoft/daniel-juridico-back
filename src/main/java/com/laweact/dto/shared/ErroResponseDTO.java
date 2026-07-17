package com.laweact.dto.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ErroResponseDTO(
        @NotNull @NotBlank String message
) {}
