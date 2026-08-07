package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.conexao.CriarConexaoInputDTO;
import com.laweact.model.enums.StatusConexaoEnum;

public interface ConexaoService {

    ConexaoResponseDTO criar(CriarConexaoInputDTO input);

    ConexaoResponseDTO cancelarDoClienteAutenticado(UUID conexaoId);

    ConexaoResponseDTO aceitarDoAdvogadoAutenticado(UUID conexaoId);

    ConexaoResponseDTO recusarDoAdvogadoAutenticado(UUID conexaoId);

    List<ConexaoResponseDTO> listarDoUsuarioAutenticado(StatusConexaoEnum status);

    List<ConexaoResponseDTO> listarPorSolicitacaoDoCliente(UUID solicitacaoId);

    ConexaoResponseDTO buscarPorAdvogadoESolicitacao(UUID advogadoId, UUID solicitacaoId);
}
