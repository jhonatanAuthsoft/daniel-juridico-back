package com.laweact.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.notificacao.NaoLidasExisteResponseDTO;
import com.laweact.dto.notificacao.NotificacaoResponseDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.NotificacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notificacoes")
@Tag(name = "Notificações", description = "Inbox de notificações do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    @Operation(summary = "Listar notificações", description = "Lista as notificações do destinatário autenticado (mais recentes primeiro)")
    public ResponseEntity<ApiResponse<List<NotificacaoResponseDTO>>> listar(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<NotificacaoResponseDTO> data = notificacaoService.listarDoUsuarioAutenticado(limit, offset);
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @GetMapping("/nao-lidas/existe")
    @Operation(summary = "Existe não lida", description = "Indica se há ao menos uma notificação não lida")
    public ResponseEntity<ApiResponse<NaoLidasExisteResponseDTO>> existeNaoLida() {
        NaoLidasExisteResponseDTO data = notificacaoService.existeNaoLida();
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @PostMapping("/{id}/ler")
    @Operation(summary = "Marcar como lida", description = "Marca uma notificação como lida (idempotente)")
    public ResponseEntity<ApiResponse<NotificacaoResponseDTO>> ler(@PathVariable UUID id) {
        NotificacaoResponseDTO data = notificacaoService.ler(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Notificação marcada como lida"));
    }

    @PostMapping("/ler-todas")
    @Operation(summary = "Marcar todas como lidas", description = "Marca todas as notificações não lidas do usuário")
    public ResponseEntity<ApiResponse<Boolean>> lerTodas() {
        notificacaoService.lerTodas();
        return ResponseEntity.ok(ApiResponse.success(true, "Notificações marcadas como lidas"));
    }
}
