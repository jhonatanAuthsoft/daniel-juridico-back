package com.laweact.dto.solicitacao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.laweact.model.enums.StatusSolicitacaoEnum;

import lombok.Builder;

@Builder
public record SolicitacaoListagemResponseDTO(
        List<SolicitacaoListagemItemDTO> items,
        Map<StatusSolicitacaoEnum, Long> contagemPorStatus
) {
    public static Map<StatusSolicitacaoEnum, Long> contagemVazia() {
        Map<StatusSolicitacaoEnum, Long> contagem = new LinkedHashMap<>();
        for (StatusSolicitacaoEnum status : StatusSolicitacaoEnum.values()) {
            contagem.put(status, 0L);
        }
        return contagem;
    }
}
