package com.laweact.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.dto.usuario.EditarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica o usuário e retorna JWT")
    public ResponseEntity<ApiResponse<LoginUsuarioResponseDTO>> login(
            @Valid @RequestBody LoginUsuarioInputDTO loginUsuarioDTO) {
        LoginUsuarioResponseDTO response = usuarioService.login(loginUsuarioDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o token JWT atual")
    public ResponseEntity<ApiResponse<Boolean>> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomError("Cabeçalho de autorização ausente ou inválido", HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST");
        }
        String token = authorizationHeader.substring(7);
        usuarioService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(true, "Operação realizada com sucesso"));
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Muda a senha do usuário")
    public ResponseEntity<ApiResponse<Boolean>> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaInputDTO redefinirSenhaInputDTO) {
        usuarioService.redefinirSenha(redefinirSenhaInputDTO);
        return ResponseEntity.ok(ApiResponse.success(true, "Operação realizada com sucesso"));
    }

    @PutMapping("/editar/{id}")
    @Operation(summary = "Edita um usuário existente")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> editar(
            @PathVariable UUID id,
            @Valid @RequestBody EditarUsuarioInputDTO input) {
        UsuarioResponseDTO response = usuarioService.editar(id, input);
        return ResponseEntity.ok(ApiResponse.success(response, "Usuário atualizado com sucesso"));
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um usuário")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable UUID id) {
        usuarioService.excluir(id);
        return new ResponseEntity<>(ApiResponse.success("Operação realizada com sucesso"), HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém detalhes de um usuário pelo ID")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> obterUsuarioPorId(@PathVariable UUID id) {
        UsuarioResponseDTO response = usuarioService.obterUsuarioPorId(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado", description = "Retorna o usuário logado")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> me() {
        UsuarioResponseDTO response = usuarioService.obterUsuarioAutenticado();
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @GetMapping
    @Operation(summary = "Obtém uma lista paginada de usuários")
    public ResponseEntity<ApiResponse<List<UsuarioResponseDTO>>> obterTodosUsuarios(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) PerfilUsuarioEnum perfil,
            @RequestParam(required = false) StatusUsuarioEnum status) {
        List<UsuarioResponseDTO> nodes = usuarioService.obterTodosUsuarios(limit, offset, searchText, perfil, status);
        long totalElements = usuarioService.contarTodosUsuarios(searchText, perfil, status);
        PaginationInfo pagination = PaginationInfo.of(limit, offset, totalElements);
        return ResponseEntity.ok(ApiResponse.success(nodes, "Consulta realizada com sucesso", pagination));
    }
}
