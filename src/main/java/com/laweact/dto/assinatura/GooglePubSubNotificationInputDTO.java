package com.laweact.dto.assinatura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record GooglePubSubNotificationInputDTO(
        @NotNull GooglePubSubMessage message
) {
    public record GooglePubSubMessage(
            @NotBlank String messageId,
            @NotBlank String data
    ) {}
}
