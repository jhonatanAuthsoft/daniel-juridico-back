package com.laweact.dto.assinatura;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AppleNotificationInputDTO(
        @NotBlank String signedPayload
) {}
