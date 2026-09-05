package com.laweact.dto.assinatura;

import java.time.LocalDateTime;

import com.laweact.model.enums.AmbienteAssinaturaEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

import lombok.Builder;

@Builder
public record AssinaturaStoreStateDTO(
        StatusAssinaturaEnum status,
        PlataformaAssinaturaEnum plataforma,
        AmbienteAssinaturaEnum ambiente,
        String productId,
        String purchaseToken,
        String originalTransactionId,
        LocalDateTime periodoFimEm,
        boolean autoRenovacao
) {}
