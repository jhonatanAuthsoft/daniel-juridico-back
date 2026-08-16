package com.laweact.dto.usuario;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AtualizarPreferenciasInputDTO(
        @NotNull(message = "notificacoesPushHabilitadas é obrigatório")
        Boolean notificacoesPushHabilitadas
) {}
