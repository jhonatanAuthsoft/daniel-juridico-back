package com.laweact.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.laweact.dto.job.NotificacaoInsistenteJobResultDTO;
import com.laweact.service.NotificacaoInsistenteService;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoInsistenteScheduler")
class NotificacaoInsistenteSchedulerTest {

    @Mock
    private NotificacaoInsistenteService notificacaoInsistenteService;

    @Test
    @DisplayName("delega o lote para o serviço de lembretes")
    void shouldDelegateToService() {
        when(notificacaoInsistenteService.processar()).thenReturn(
                NotificacaoInsistenteJobResultDTO.builder()
                        .avaliadas(1)
                        .reenviadas(1)
                        .ignoradas(0)
                        .erros(0)
                        .build()
        );

        NotificacaoInsistenteScheduler scheduler =
                new NotificacaoInsistenteScheduler(notificacaoInsistenteService);

        scheduler.processarLembretes();

        verify(notificacaoInsistenteService).processar();
    }

    @Test
    @DisplayName("roda a cada hora via cron configurável")
    void shouldBeScheduledEveryHour() throws Exception {
        Method method = NotificacaoInsistenteScheduler.class.getMethod("processarLembretes");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${laweact.notificacoes.insistente.cron:0 0 * * * *}");
    }
}
