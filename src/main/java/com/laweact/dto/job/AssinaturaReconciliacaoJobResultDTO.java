package com.laweact.dto.job;

import lombok.Builder;

@Builder
public record AssinaturaReconciliacaoJobResultDTO(
        int assinaturasExpiradas,
        int assinaturasSincronizadas
) {}
