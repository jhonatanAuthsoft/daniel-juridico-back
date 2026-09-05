package com.laweact.dto.assinatura;

import com.laweact.model.enums.PlataformaAssinaturaEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ValidarAssinaturaInputDTO(
        @NotNull PlataformaAssinaturaEnum plataforma,
        @NotBlank String productId,
        @NotBlank String purchaseToken
) {}
