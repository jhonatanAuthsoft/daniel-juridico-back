package com.laweact.dto.dispositivo;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RemoverDispositivoPushInputDTO(
        @NotBlank(message = "expoPushToken é obrigatório")
        String expoPushToken
) {}
