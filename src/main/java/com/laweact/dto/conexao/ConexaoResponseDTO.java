package com.laweact.dto.conexao;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.StatusConexaoEnum;

import lombok.Builder;

@Builder
public record ConexaoResponseDTO(
        UUID id,
        UUID solicitacaoId,
        UUID clienteId,
        UUID advogadoId,
        StatusConexaoEnum status,
        LocalDateTime criadoEm,
        LocalDateTime decididoEm,
        LocalDateTime canceladoEm,
        String telefone,
        String email,
        String nomeAdvogado,
        String nomeCliente,
        String tituloSolicitacao
) {}
