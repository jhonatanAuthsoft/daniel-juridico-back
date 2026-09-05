package com.laweact.dto.conexao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

import lombok.Builder;

@Builder
public record ConexaoListagemResponseDTO(
        List<ConexaoResponseDTO> items,
        Map<UrgenciaSolicitacaoEnum, Long> contagemPorUrgencia,
        Map<StatusConexaoEnum, Long> contagemPorStatus
) {
    public static Map<UrgenciaSolicitacaoEnum, Long> contagemVazia() {
        Map<UrgenciaSolicitacaoEnum, Long> contagem = new LinkedHashMap<>();
        for (UrgenciaSolicitacaoEnum urgencia : UrgenciaSolicitacaoEnum.values()) {
            contagem.put(urgencia, 0L);
        }
        return contagem;
    }

    public static Map<StatusConexaoEnum, Long> contagemStatusVazia() {
        Map<StatusConexaoEnum, Long> contagem = new LinkedHashMap<>();
        for (StatusConexaoEnum status : StatusConexaoEnum.values()) {
            contagem.put(status, 0L);
        }
        return contagem;
    }
}
