package com.laweact.dto.dispositivo;

import com.laweact.model.enums.PlataformaDispositivoEnum;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RegistrarDispositivoPushInputDTO(
        @NotBlank(message = "expoPushToken é obrigatório")
        String expoPushToken,

        PlataformaDispositivoEnum plataforma
) {}
