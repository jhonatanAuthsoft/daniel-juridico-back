package com.laweact.service;

import java.util.UUID;

import com.laweact.dto.shared.PaginationInfo;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoListagemResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoMatchResponseDTO;
import com.laweact.model.enums.StatusSolicitacaoEnum;

import java.util.List;

public interface SolicitacaoService {

    CriarSolicitacaoResponseDTO criar(CriarSolicitacaoInputDTO input);

    CriarSolicitacaoResponseDTO buscarDoClienteAutenticado(UUID solicitacaoId);

    CriarSolicitacaoResponseDTO cancelarDoClienteAutenticado(UUID solicitacaoId);

    List<SolicitacaoMatchResponseDTO> listarMatches(UUID solicitacaoId);

    record ListagemPaginada(SolicitacaoListagemResponseDTO data, PaginationInfo pagination) {}

    ListagemPaginada listarDoClienteAutenticado(
            int limit,
            int offset,
            StatusSolicitacaoEnum status,
            String busca
    );
}
