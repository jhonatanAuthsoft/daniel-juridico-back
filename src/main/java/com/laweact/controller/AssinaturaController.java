package com.laweact.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.dto.assinatura.ValidarAssinaturaInputDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.AssinaturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/assinaturas")
@Tag(name = "Assinaturas", description = "Assinatura mensal do advogado via lojas de aplicativo")
@SecurityRequirement(name = "bearerAuth")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping("/me")
    @Operation(summary = "Minha assinatura", description = "Retorna o estado da assinatura do usuário autenticado")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> minhaAssinatura() {
        AssinaturaResponseDTO data = assinaturaService.obterMinhaAssinatura();
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @PostMapping("/validar")
    @Operation(summary = "Validar compra", description = "Valida o recibo/token da loja e ativa a assinatura")
    public ResponseEntity<ApiResponse<AssinaturaResponseDTO>> validar(
            @Valid @RequestBody ValidarAssinaturaInputDTO input
    ) {
        AssinaturaResponseDTO data = assinaturaService.validarCompra(input);
        return ResponseEntity.ok(ApiResponse.success(data, "Assinatura validada com sucesso"));
    }
}
