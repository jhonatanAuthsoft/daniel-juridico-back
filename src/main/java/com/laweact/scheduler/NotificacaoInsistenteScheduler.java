package com.laweact.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.laweact.dto.job.NotificacaoInsistenteJobResultDTO;
import com.laweact.service.NotificacaoInsistenteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "laweact.notificacoes.insistente",
        name = "scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificacaoInsistenteScheduler {

    private final NotificacaoInsistenteService notificacaoInsistenteService;

    @Scheduled(cron = "${laweact.notificacoes.insistente.cron:0 0 * * * *}")
    public void processarLembretes() {
        log.info("Iniciando job de notificações insistentes");
        NotificacaoInsistenteJobResultDTO result = notificacaoInsistenteService.processar();
        log.info(
                "Job de notificações insistentes concluído: avaliadas={} reenviadas={} ignoradas={} erros={}",
                result.avaliadas(),
                result.reenviadas(),
                result.ignoradas(),
                result.erros()
        );
    }
}
