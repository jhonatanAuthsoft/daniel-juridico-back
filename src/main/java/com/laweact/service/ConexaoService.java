package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.conexao.ConexaoListagemResponseDTO;
import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.conexao.CriarConexaoInputDTO;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

public interface ConexaoService {

    ConexaoResponseDTO criar(CriarConexaoInputDTO input);

    ConexaoResponseDTO cancelarDoClienteAutenticado(UUID conexaoId);

    ConexaoResponseDTO aceitarDoAdvogadoAutenticado(UUID conexaoId);

    ConexaoResponseDTO recusarDoAdvogadoAutenticado(UUID conexaoId);

    /**
     * Registra a primeira abertura da solicitação pelo advogado dono da conexão.
     * Idempotente: chamadas seguintes preservam a data original.
     */
    ConexaoResponseDTO marcarVisualizadaDoAdvogadoAutenticado(UUID conexaoId);

    record ListagemPaginada(ConexaoListagemResponseDTO data, PaginationInfo pagination) {}

    /**
     * Inbox do usuário autenticado. {@code limit <= 0} devolve a lista inteira
     * sem paginar, comportamento usado por telas que precisam do conjunto todo.
     */
    ListagemPaginada listarDoUsuarioAutenticado(
            int limit,
            int offset,
            List<StatusConexaoEnum> status,
            UrgenciaSolicitacaoEnum urgencia,
            String busca
    );

    List<ConexaoResponseDTO> listarPorSolicitacaoDoCliente(UUID solicitacaoId);

    ConexaoResponseDTO buscarPorAdvogadoESolicitacao(UUID advogadoId, UUID solicitacaoId);
}
