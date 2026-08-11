package com.laweact.service;

import java.util.List;

import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.entity.SolicitacaoMatchEntity;

public interface MatchingService {

    /**
     * Calcula e persiste o ranking de advogados compatíveis com a solicitação.
     * Executado uma única vez, na criação (sem recálculo posterior).
     */
    List<SolicitacaoMatchEntity> gerarMatches(SolicitacaoEntity solicitacao);
}
