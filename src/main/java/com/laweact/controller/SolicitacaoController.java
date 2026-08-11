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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.shared.ApiResponse;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.solicitacao.CriarSolicitacaoResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoListagemResponseDTO;
import com.laweact.dto.solicitacao.SolicitacaoMatchResponseDTO;
import com.laweact.model.enums.StatusSolicitacaoEnum;
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

    @GetMapping
    @Operation(
            summary = "Listar solicitações do cliente",
            description = "Retorna as solicitações do cliente autenticado, com filtro opcional por status, "
                    + "paginação de 10 em 10 e contagem global por status para os bullets"
    )
    public ResponseEntity<ApiResponse<SolicitacaoListagemResponseDTO>> listar(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) StatusSolicitacaoEnum status
    ) {
        SolicitacaoService.ListagemPaginada listagem =
                solicitacaoService.listarDoClienteAutenticado(limit, offset, status);
        return ResponseEntity.ok(ApiResponse.success(
                listagem.data(),
                "Consulta realizada com sucesso",
                listagem.pagination()
        ));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar solicitação por ID",
            description = "Retorna os detalhes da solicitação do cliente autenticado"
    )
    public ResponseEntity<ApiResponse<CriarSolicitacaoResponseDTO>> buscar(@PathVariable UUID id) {
        CriarSolicitacaoResponseDTO data = solicitacaoService.buscarDoClienteAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(
            summary = "Cancelar solicitação",
            description = "Cancela a solicitação do cliente autenticado. "
                    + "Não permitido para status CANCELADA ou ENCERRADA"
    )
    public ResponseEntity<ApiResponse<CriarSolicitacaoResponseDTO>> cancelar(@PathVariable UUID id) {
        CriarSolicitacaoResponseDTO data = solicitacaoService.cancelarDoClienteAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Solicitação cancelada com sucesso"));
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
