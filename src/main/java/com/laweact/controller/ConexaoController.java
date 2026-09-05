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

import com.laweact.dto.conexao.ConexaoListagemResponseDTO;
import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.conexao.CriarConexaoInputDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;
import com.laweact.service.ConexaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conexoes")
@Tag(name = "Conexões", description = "Pedidos de conexão cliente ↔ advogado vinculados a uma solicitação")
@SecurityRequirement(name = "bearerAuth")
public class ConexaoController {

    private final ConexaoService conexaoService;

    @PostMapping
    @Operation(summary = "Solicitar conexão", description = "Cliente cria (ou reativa após cancelamento) um pedido PENDENTE")
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> criar(
            @Valid @RequestBody CriarConexaoInputDTO input
    ) {
        ConexaoResponseDTO data = conexaoService.criar(input);
        return new ResponseEntity<>(
                ApiResponse.success(data, "Conexão solicitada com sucesso"),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar conexão", description = "Cliente cancela enquanto PENDENTE (pode solicitar de novo)")
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> cancelar(@PathVariable UUID id) {
        ConexaoResponseDTO data = conexaoService.cancelarDoClienteAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Conexão cancelada com sucesso"));
    }

    @PostMapping("/{id}/aceitar")
    @Operation(summary = "Aceitar conexão", description = "Advogado aceita e libera contato")
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> aceitar(@PathVariable UUID id) {
        ConexaoResponseDTO data = conexaoService.aceitarDoAdvogadoAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Conexão aceita com sucesso"));
    }

    @PostMapping("/{id}/recusar")
    @Operation(summary = "Recusar conexão", description = "Advogado recusa; cliente não pode reenviar neste par")
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> recusar(@PathVariable UUID id) {
        ConexaoResponseDTO data = conexaoService.recusarDoAdvogadoAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Conexão recusada com sucesso"));
    }

    @PostMapping("/{id}/visualizar")
    @Operation(
            summary = "Marcar conexão como visualizada",
            description = "Advogado registra a primeira abertura da solicitação. Idempotente: preserva a data original"
    )
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> visualizar(@PathVariable UUID id) {
        ConexaoResponseDTO data = conexaoService.marcarVisualizadaDoAdvogadoAutenticado(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Conexão marcada como visualizada"));
    }

    @GetMapping
    @Operation(
            summary = "Listar conexões",
            description = "Inbox do cliente ou advogado autenticado, com filtros opcionais por status (repetível), "
                    + "grau de urgência e busca livre (nome da contraparte, título, descrição e cidade). Para o "
                    + "advogado a ordenação coloca as emergências no topo. Retorna a contagem global por urgência "
                    + "e por status para os filtros da listagem. Sem `limit` a lista vem inteira, sem paginar"
    )
    public ResponseEntity<ApiResponse<ConexaoListagemResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) List<StatusConexaoEnum> status,
            @RequestParam(required = false) UrgenciaSolicitacaoEnum urgencia,
            @RequestParam(required = false) String busca
    ) {
        ConexaoService.ListagemPaginada listagem =
                conexaoService.listarDoUsuarioAutenticado(limit, offset, status, urgencia, busca);
        return ResponseEntity.ok(ApiResponse.success(
                listagem.data(),
                "Consulta realizada com sucesso",
                listagem.pagination()
        ));
    }
}
