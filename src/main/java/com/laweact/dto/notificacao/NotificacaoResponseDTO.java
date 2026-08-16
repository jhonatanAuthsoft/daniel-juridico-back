package com.laweact.dto.notificacao;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.ReferenciaNotificacaoEnum;
import com.laweact.model.enums.StatusEnvioNotificacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record NotificacaoResponseDTO(
        @NotNull UUID id,
        @NotNull @NotBlank String titulo,
        @NotNull @NotBlank String texto,
        @NotNull TipoNotificacaoEnum tipo,
        @NotNull ReferenciaNotificacaoEnum referenciaTipo,
        @NotNull UUID referenciaId,
        @NotNull UUID remetenteId,
        @NotNull LocalDateTime criadoEm,
        LocalDateTime lidaEm,
        @NotNull StatusEnvioNotificacaoEnum statusEnvio
) {}
