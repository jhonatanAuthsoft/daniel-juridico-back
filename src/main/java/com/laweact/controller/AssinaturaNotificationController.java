package com.laweact.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.assinatura.AppleNotificationInputDTO;
import com.laweact.dto.assinatura.GooglePubSubNotificationInputDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.AssinaturaNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/assinaturas/notificacoes")
@Tag(name = "Assinaturas webhooks", description = "Notificações server-to-server das lojas")
public class AssinaturaNotificationController {

    private final AssinaturaNotificationService assinaturaNotificationService;

    @PostMapping("/apple")
    @Operation(summary = "Webhook Apple", description = "App Store Server Notifications V2")
    public ResponseEntity<ApiResponse<Void>> apple(@Valid @RequestBody AppleNotificationInputDTO input) {
        assinaturaNotificationService.processarApple(input);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificação processada"));
    }

    @PostMapping("/google")
    @Operation(summary = "Webhook Google", description = "Real-time Developer Notifications via Pub/Sub push")
    public ResponseEntity<ApiResponse<Void>> google(@Valid @RequestBody GooglePubSubNotificationInputDTO input) {
        assinaturaNotificationService.processarGoogle(input);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificação processada"));
    }
}
