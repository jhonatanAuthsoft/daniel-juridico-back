package com.laweact.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.shared.ApiResponse;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoMatchResponseDTO;
import com.laweact.service.SolicitacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/solicitacoes")
@Tag(name = "Solicitações", description = "Demandas jurídicas do cliente")
@SecurityRequirement(name = "bearerAuth")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    @PostMapping
    @Operation(
            summary = "Criar solicitação",
            description = "Persiste a demanda do cliente autenticado e calcula o ranking de advogados compatíveis"
    )
    public ResponseEntity<ApiResponse<CriarSolicitacaoResponseDTO>> criar(
            @Valid @RequestBody CriarSolicitacaoInputDTO input
    ) {
        CriarSolicitacaoResponseDTO response = solicitacaoService.criar(input);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Solicitação criada com sucesso"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}/matches")
    @Operation(
            summary = "Listar advogados compatíveis",
            description = "Ranking persistido na criação, ordenado por compatibilidade e proximidade"
    )
    public ResponseEntity<ApiResponse<List<SolicitacaoMatchResponseDTO>>> listarMatches(
            @PathVariable UUID id
    ) {
        List<SolicitacaoMatchResponseDTO> data = solicitacaoService.listarMatches(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }
}
