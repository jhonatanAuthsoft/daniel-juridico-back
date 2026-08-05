package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoMatchResponseDTO;

public interface SolicitacaoService {

    CriarSolicitacaoResponseDTO criar(CriarSolicitacaoInputDTO input);

    List<SolicitacaoMatchResponseDTO> listarMatches(UUID solicitacaoId);
}
