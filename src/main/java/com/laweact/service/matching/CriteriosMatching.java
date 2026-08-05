package com.laweact.service.matching;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

import lombok.Builder;

/** Critérios da solicitação usados no cálculo de compatibilidade. */
@Builder
public record CriteriosMatching(
        ModalidadeSolicitacaoEnum modalidade,
        String especialidadeCodigo,
        String subespecialidadeCodigo,
        String uf,
        String cidade,
        UrgenciaSolicitacaoEnum urgencia,
        FormaCobrancaSolicitacaoEnum formaCobranca,
        Integer experienciaMinimaMeses
) {}
