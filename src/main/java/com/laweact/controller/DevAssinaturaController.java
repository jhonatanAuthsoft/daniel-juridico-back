package com.laweact.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AssinaturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev/assinaturas")
@Tag(name = "Dev assinaturas", description = "Endpoints de teste para simular ciclo de assinatura")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(name = "laweact.assinatura.fake-store.enabled", havingValue = "true")
public class DevAssinaturaController {

    private final AssinaturaService assinaturaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/expirar-trial")
    @Operation(summary = "Expirar trial", description = "Força o fim do período de testes do usuário autenticado")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> expirarTrial() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        assinaturaService.expirarTrial(usuario.getId());
        return ResponseEntity.ok(ApiResponse.success(
                assinaturaService.obterAssinaturaDoUsuario(usuario),
                "Trial expirado"
        ));
    }

    @PostMapping("/expirar-assinatura")
    @Operation(summary = "Expirar assinatura", description = "Força o fim da assinatura ativa do usuário autenticado")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> expirarAssinatura() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        assinaturaService.expirarAssinatura(usuario.getId());
        return ResponseEntity.ok(ApiResponse.success(
                assinaturaService.obterAssinaturaDoUsuario(usuario),
                "Assinatura expirada"
        ));
    }

    @PostMapping("/renovar")
    @Operation(summary = "Renovar assinatura", description = "Simula renovação da assinatura (modo fake)")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> renovar() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        assinaturaService.renovarAssinatura(usuario.getId());
        return ResponseEntity.ok(ApiResponse.success(
                assinaturaService.obterAssinaturaDoUsuario(usuario),
                "Assinatura renovada"
        ));
    }

    @PostMapping("/reset")
    @Operation(summary = "Resetar assinatura", description = "Volta o usuário ao estado TRIAL inicial")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> reset() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        assinaturaService.resetAssinatura(usuario.getId());
        return ResponseEntity.ok(ApiResponse.success(
                assinaturaService.obterAssinaturaDoUsuario(usuario),
                "Assinatura resetada"
        ));
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
