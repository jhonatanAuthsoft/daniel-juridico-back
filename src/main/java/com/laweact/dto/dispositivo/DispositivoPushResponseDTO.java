package com.laweact.dto.dispositivo;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.PlataformaDispositivoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DispositivoPushResponseDTO(
        @NotNull UUID id,
        @NotNull @NotBlank String expoPushToken,
        PlataformaDispositivoEnum plataforma,
        @NotNull Boolean ativo,
        @NotNull LocalDateTime ultimoRegistroEm
) {}
