package com.laweact.dto.solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

import lombok.Builder;

@Builder
public record CriarSolicitacaoResponseDTO(
        UUID id,
        StatusSolicitacaoEnum status,
        String titulo,
        ModalidadeSolicitacaoEnum modalidade,
        String especialidadeCodigo,
        String subespecialidadeCodigo,
        String uf,
        String cidade,
        UrgenciaSolicitacaoEnum urgencia,
        String descricao,
        FormaCobrancaSolicitacaoEnum formaCobranca,
        Integer experienciaMinimaMeses,
        /** Quantidade de advogados compatíveis calculada na criação. */
        Integer totalMatches,
        LocalDateTime criadoEm
) {}
