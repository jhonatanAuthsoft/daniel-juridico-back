package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.dispositivo.DispositivoPushResponseDTO;
import com.laweact.dto.dispositivo.RegistrarDispositivoPushInputDTO;
import com.laweact.dto.dispositivo.RemoverDispositivoPushInputDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.DispositivoPushService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dispositivos-push")
@Tag(name = "Dispositivos push", description = "Registro de tokens Expo Push do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class DispositivoPushController {

    private final DispositivoPushService dispositivoPushService;

    @PostMapping
    @Operation(summary = "Registrar dispositivo", description = "Upsert do token Expo Push para o usuário autenticado")
    public ResponseEntity<ApiResponse<DispositivoPushResponseDTO>> registrar(
            @Valid @RequestBody RegistrarDispositivoPushInputDTO input
    ) {
        DispositivoPushResponseDTO data = dispositivoPushService.registrar(input);
        return new ResponseEntity<>(
                ApiResponse.success(data, "Dispositivo registrado com sucesso"),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping
    @Operation(summary = "Remover dispositivo", description = "Desativa (soft) o token Expo Push do usuário autenticado")
    public ResponseEntity<ApiResponse<DispositivoPushResponseDTO>> remover(
            @Valid @RequestBody RemoverDispositivoPushInputDTO input
    ) {
        DispositivoPushResponseDTO data = dispositivoPushService.remover(input);
        return ResponseEntity.ok(ApiResponse.success(data, "Dispositivo removido com sucesso"));
    }
}
