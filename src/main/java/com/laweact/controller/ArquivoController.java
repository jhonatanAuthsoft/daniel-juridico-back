package com.laweact.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.arquivo.ArquivoUrlLeituraInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlLeituraResponseDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadResponseDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.ArquivoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/arquivos")
@Tag(name = "Arquivos", description = "URLs assinadas para upload/leitura no S3")
public class ArquivoController {

    private final ArquivoService arquivoService;

    @PostMapping("/url-upload")
    @Operation(
            summary = "Gerar URL de upload",
            description = "Retorna key + URL assinada (PUT) para o app enviar a imagem direto ao S3"
    )
    public ResponseEntity<ApiResponse<ArquivoUrlUploadResponseDTO>> criarUrlUpload(
            @Valid @RequestBody ArquivoUrlUploadInputDTO input
    ) {
        ArquivoUrlUploadResponseDTO data = arquivoService.criarUrlUpload(input);
        return ResponseEntity.ok(ApiResponse.success(data, "URL de upload gerada com sucesso"));
    }

    @PostMapping("/url-leitura")
    @Operation(
            summary = "Gerar URL de leitura",
            description = "Retorna URL assinada (GET) para uma key já persistida"
    )
    public ResponseEntity<ApiResponse<ArquivoUrlLeituraResponseDTO>> criarUrlLeitura(
            @Valid @RequestBody ArquivoUrlLeituraInputDTO input
    ) {
        ArquivoUrlLeituraResponseDTO data = arquivoService.criarUrlLeitura(input);
        return ResponseEntity.ok(ApiResponse.success(data, "URL de leitura gerada com sucesso"));
    }
}
