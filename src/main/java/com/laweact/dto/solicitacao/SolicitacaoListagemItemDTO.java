package com.laweact.dto.solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

import lombok.Builder;

@Builder
public record SolicitacaoListagemItemDTO(
        UUID id,
        StatusSolicitacaoEnum status,
        UrgenciaSolicitacaoEnum urgencia,
        String titulo,
        String descricao,
        LocalDateTime dataAbertura,
        String especialidadeCodigo,
        String especialidade,
        Integer totalMatches
) {}
