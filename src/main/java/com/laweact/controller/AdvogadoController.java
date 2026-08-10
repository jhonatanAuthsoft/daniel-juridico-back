package com.laweact.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.advogado.AdvogadoPerfilPublicoResponseDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoItemResponseDTO;
import com.laweact.dto.avaliacao.AvaliacaoListagemResponseDTO;
import com.laweact.dto.avaliacao.CriarAvaliacaoInputDTO;
import com.laweact.dto.conexao.ConexaoResponseDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.AdvogadoService;
import com.laweact.service.ConexaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/advogados")
@Tag(name = "Advogados", description = "Cadastro e perfil de advogados")
public class AdvogadoController {

    private final AdvogadoService advogadoService;
    private final ConexaoService conexaoService;

    @PostMapping("/cadastrar")
    @Operation(
            summary = "Cadastrar advogado",
            description = "Cria usuário + perfil de advogado + endereço + OAB(s) + áreas de atuação e retorna JWT"
    )
    public ResponseEntity<ApiResponse<CadastrarAdvogadoResponseDTO>> cadastrar(
            @Valid @RequestBody CadastrarAdvogadoInputDTO input
    ) {
        CadastrarAdvogadoResponseDTO response = advogadoService.cadastrar(input);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Advogado cadastrado com sucesso"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Perfil público do advogado",
            description = "Retorna os dados exibíveis ao cliente na tela de visualizar perfil "
                    + "(sem CPF, RG, telefone, e-mail ou fotos de OAB)"
    )
    public ResponseEntity<ApiResponse<AdvogadoPerfilPublicoResponseDTO>> obterPerfilPublico(
            @PathVariable UUID id
    ) {
        AdvogadoPerfilPublicoResponseDTO data = advogadoService.obterPerfilPublico(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @GetMapping("/{id}/conexao")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Status da conexão com o advogado",
            description = "Retorna a conexão do cliente autenticado com este advogado na solicitação informada"
    )
    public ResponseEntity<ApiResponse<ConexaoResponseDTO>> obterConexao(
            @PathVariable UUID id,
            @RequestParam UUID solicitacaoId
    ) {
        ConexaoResponseDTO data = conexaoService.buscarPorAdvogadoESolicitacao(id, solicitacaoId);
        return ResponseEntity.ok(ApiResponse.success(data, "Consulta realizada com sucesso"));
    }

    @GetMapping("/{id}/avaliacoes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar avaliações do advogado",
            description = "Retorna avaliações paginadas: a do usuário autenticado primeiro (se houver), "
                    + "demais por mais recentes. Média e total. Nota válida: 0.5 a 5.0."
    )
    public ResponseEntity<ApiResponse<AvaliacaoListagemResponseDTO>> listarAvaliacoes(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        AdvogadoService.AvaliacoesPaginadas listagem = advogadoService.listarAvaliacoes(id, limit, offset);
        return ResponseEntity.ok(ApiResponse.success(
                listagem.data(),
                "Consulta realizada com sucesso",
                listagem.pagination()
        ));
    }

    @PostMapping("/{id}/avaliacoes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Criar avaliação",
            description = "Cliente com conexão ACEITA cria uma avaliação (nota + comentário). "
                    + "Uma avaliação por par cliente/advogado."
    )
    public ResponseEntity<ApiResponse<AvaliacaoItemResponseDTO>> criarAvaliacao(
            @PathVariable UUID id,
            @Valid @RequestBody CriarAvaliacaoInputDTO input
    ) {
        AvaliacaoItemResponseDTO data = advogadoService.criarAvaliacao(id, input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Avaliação criada com sucesso"));
    }

    @DeleteMapping("/{id}/avaliacoes/{avaliacaoId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Excluir avaliação",
            description = "Remove a avaliação. Apenas o cliente que criou pode excluir."
    )
    public ResponseEntity<ApiResponse<Map<String, UUID>>> excluirAvaliacao(
            @PathVariable UUID id,
            @PathVariable UUID avaliacaoId
    ) {
        UUID excluidaId = advogadoService.excluirAvaliacao(id, avaliacaoId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("id", excluidaId),
                "Avaliação excluída com sucesso"
        ));
    }
}
