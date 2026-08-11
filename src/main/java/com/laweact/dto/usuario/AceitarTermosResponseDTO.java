package com.laweact.dto.usuario;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AceitarTermosResponseDTO(
        @NotNull UUID id,
        @NotNull UUID usuarioId,
        @NotBlank String versao,
        @NotNull Boolean scrollConfirmado,
        @NotNull Boolean checkboxConfirmado,
        @NotNull LocalDateTime aceitoEm,
        @NotNull Boolean termosAceitos
) {}
