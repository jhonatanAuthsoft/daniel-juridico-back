package com.laweact.model.enums;

/**
 * Status da solicitação do cliente.
 * {@code AGUARDANDO_MATCHING} = pendente (chip Pendentes) até o primeiro aceite de conexão.
 */
public enum StatusSolicitacaoEnum {
    AGUARDANDO_MATCHING,
    MATCH_REALIZADO,
    CANCELADA
}
