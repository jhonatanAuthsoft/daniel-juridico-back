package com.laweact.dto.job;

import lombok.Builder;

@Builder
public record AssinaturaReconciliacaoJobResultDTO(
        int trialsExpirados,
        int assinaturasExpiradas,
        int assinaturasSincronizadas
) {}
