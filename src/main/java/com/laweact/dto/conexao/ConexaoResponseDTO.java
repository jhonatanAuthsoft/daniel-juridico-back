package com.laweact.dto.conexao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

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
        String tituloSolicitacao,
        String descricaoSolicitacao,
        UrgenciaSolicitacaoEnum urgencia,
        ModalidadeSolicitacaoEnum modalidade,
        String especialidadeCodigo,
        String subespecialidadeCodigo,
        Integer experienciaMinimaMeses,
        String uf,
        String cidade,
        FormaCobrancaSolicitacaoEnum formaCobranca,
        String clienteProfissao,
        String clientePronomes,
        String clienteEstadoCivil,
        String clienteFaixaRenda,
        String clienteFotoUrl,
        String clienteCidade,
        String clienteUf,
        String clienteTelefone,
        String clienteEmail,
        BigDecimal avaliacaoClienteNota,
        String avaliacaoClienteComentario
) {}
