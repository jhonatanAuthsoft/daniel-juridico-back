package com.laweact.dto.assinatura;

import java.time.LocalDateTime;

import com.laweact.model.enums.AmbienteAssinaturaEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AssinaturaResponseDTO(
        @NotNull StatusAssinaturaEnum status,
        @NotNull Boolean acessoLiberado,
        LocalDateTime periodoFimEm,
        PlataformaAssinaturaEnum plataforma,
        AmbienteAssinaturaEnum ambiente,
        String productId,
        @NotNull Boolean autoRenovacao
) {}
