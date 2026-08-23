package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.config.JobsApiKeyProperties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.job.NotificacaoInsistenteJobResultDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.NotificacaoInsistenteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jobs")
@Tag(name = "Jobs", description = "Endpoints disparados pela infra (cron)")
public class JobsController {

    private final JobsApiKeyProperties jobsApiKeyProperties;
    private final NotificacaoInsistenteService notificacaoInsistenteService;

    @PostMapping("/notificacoes-insistentes")
    @Operation(
            summary = "Processar notificações insistentes",
            description = "Reenvia lembretes de conexões PENDENTE com urgência EMERGENCIA/URGENTE. Auth via X-Api-Key."
    )
    public ResponseEntity<ApiResponse<NotificacaoInsistenteJobResultDTO>> processarNotificacoesInsistentes(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey
    ) {
        if (!jobsApiKeyProperties.matches(apiKey)) {
            throw new CustomError("API key inválida", HttpStatus.UNAUTHORIZED);
        }

        NotificacaoInsistenteJobResultDTO data = notificacaoInsistenteService.processar();
        return ResponseEntity.ok(ApiResponse.success(data, "Lote processado"));
    }
}
