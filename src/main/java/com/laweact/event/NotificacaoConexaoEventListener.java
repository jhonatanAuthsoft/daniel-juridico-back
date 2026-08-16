package com.laweact.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.service.NotificacaoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacaoConexaoEventListener {

    private final NotificacaoService notificacaoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitada(ConexaoSolicitadaEvent event) {
        try {
            notificacaoService.criarETentarEnviar(
                    event.advogadoUsuarioId(),
                    event.clienteUsuarioId(),
                    TipoNotificacaoEnum.CONEXAO_SOLICITADA,
                    event.conexaoId(),
                    "Nova solicitação de conexão",
                    event.nomeCliente() + " solicitou conexão sobre \"" + event.tituloSolicitacao() + "\""
            );
        } catch (Exception ex) {
            log.error(
                    "Falha ao criar notificação CONEXAO_SOLICITADA para conexão {}",
                    event.conexaoId(),
                    ex
            );
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAceita(ConexaoAceitaEvent event) {
        try {
            notificacaoService.criarETentarEnviar(
                    event.clienteUsuarioId(),
                    event.advogadoUsuarioId(),
                    TipoNotificacaoEnum.CONEXAO_ACEITA,
                    event.conexaoId(),
                    "Conexão aceita",
                    event.nomeAdvogado() + " aceitou sua solicitação \"" + event.tituloSolicitacao() + "\""
            );
        } catch (Exception ex) {
            log.error(
                    "Falha ao criar notificação CONEXAO_ACEITA para conexão {}",
                    event.conexaoId(),
                    ex
            );
        }
    }
}
