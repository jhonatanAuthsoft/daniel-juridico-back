package com.laweact.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.dto.shared.PaginationInfo;
import com.laweact.dto.usuario.AceitarTermosInputDTO;
import com.laweact.dto.usuario.AceitarTermosResponseDTO;
import com.laweact.dto.usuario.AtualizarPreferenciasInputDTO;
import com.laweact.dto.usuario.EmailDisponivelResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
import com.laweact.dto.usuario.PreferenciasResponseDTO;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.RedefinirSenhaResponseDTO;
import com.laweact.dto.usuario.RefreshTokenInputDTO;
import com.laweact.dto.usuario.RefreshTokenResponseDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaInputDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoInputDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoResponseDTO;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.service.RecuperacaoSenhaService;
import com.laweact.service.TermosAceiteService;
import com.laweact.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    private final TermosAceiteService termosAceiteService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica o usuário e retorna access token (1h) + refresh token (7d)")
    public ResponseEntity<ApiResponse<LoginUsuarioResponseDTO>> login(
            @Valid @RequestBody LoginUsuarioInputDTO loginUsuarioDTO) {
        LoginUsuarioResponseDTO response = usuarioService.login(loginUsuarioDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh token",
            description = "Recebe access token + refresh token e devolve um novo par (access 1h, refresh 7d)"
    )
    public ResponseEntity<ApiResponse<RefreshTokenResponseDTO>> refresh(
            @Valid @RequestBody RefreshTokenInputDTO input
    ) {
        RefreshTokenResponseDTO response = usuarioService.refresh(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Token atualizado com sucesso"));
    }

    @PostMapping("/recuperar-senha")
    @Operation(
            summary = "Solicitar recuperação de senha",
            description = "Gera código de 4 dígitos e envia por e-mail (resposta genérica se a conta existir ou não)"
    )
    public ResponseEntity<ApiResponse<SolicitarRecuperacaoSenhaResponseDTO>> solicitarRecuperacaoSenha(
            @Valid @RequestBody SolicitarRecuperacaoSenhaInputDTO input
    ) {
        SolicitarRecuperacaoSenhaResponseDTO response = recuperacaoSenhaService.solicitarCodigo(input);
        return ResponseEntity.ok(ApiResponse.success(response, response.mensagem()));
    }

    @PostMapping("/validar-codigo-recuperacao")
    @Operation(summary = "Validar código de recuperação", description = "Valida o código sem consumi-lo")
    public ResponseEntity<ApiResponse<ValidarCodigoRecuperacaoResponseDTO>> validarCodigoRecuperacao(
            @Valid @RequestBody ValidarCodigoRecuperacaoInputDTO input
    ) {
        ValidarCodigoRecuperacaoResponseDTO response = recuperacaoSenhaService.validarCodigo(input);
        return ResponseEntity.ok(ApiResponse.success(response, response.mensagem()));
    }

    @PostMapping("/redefinir-senha")
    @Operation(
            summary = "Redefinir senha",
            description = "Troca a senha com código válido e invalida sessões ativas"
    )
    public ResponseEntity<ApiResponse<RedefinirSenhaResponseDTO>> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaInputDTO input
    ) {
        RedefinirSenhaResponseDTO response = recuperacaoSenhaService.redefinirSenha(input);
        return ResponseEntity.ok(ApiResponse.success(response, response.mensagem()));
    }

    @PostMapping("/aceitar-termos")
    @Operation(
            summary = "Aceitar termos de uso",
            description = "Registra o aceite dos termos para o usuário autenticado (usuário, versão e data)"
    )
    public ResponseEntity<ApiResponse<AceitarTermosResponseDTO>> aceitarTermos(
            @Valid @RequestBody AceitarTermosInputDTO input
    ) {
        AceitarTermosResponseDTO response = termosAceiteService.aceitar(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Termos aceitos com sucesso"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o token JWT atual")
    public ResponseEntity<ApiResponse<Boolean>> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomError(
                    "Cabeçalho de autorização ausente ou inválido",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REQUEST"
            );
        }
        String token = authorizationHeader.substring(7);
        usuarioService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(true, "Operação realizada com sucesso"));
    }

    @GetMapping("/email-disponivel")
    @Operation(
            summary = "Verificar disponibilidade de e-mail",
            description = "Indica se o e-mail já está cadastrado (uso no cadastro de cliente/advogado)"
    )
    public ResponseEntity<ApiResponse<EmailDisponivelResponseDTO>> verificarEmailDisponivel(
            @RequestParam String email
    ) {
        EmailDisponivelResponseDTO response = usuarioService.verificarEmailDisponivel(email);
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado", description = "Retorna o usuário logado com detalhe do perfil (cliente ou advogado)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<MeResponseDTO>> me() {
        MeResponseDTO response = usuarioService.obterUsuarioAutenticado();
        return ResponseEntity.ok(ApiResponse.success(response, "Consulta realizada com sucesso"));
    }

    @PatchMapping("/me/preferencias")
    @Operation(
            summary = "Atualizar preferências",
            description = "Atualiza preferências do usuário autenticado (ex.: notificações push)"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<PreferenciasResponseDTO>> atualizarPreferencias(
            @Valid @RequestBody AtualizarPreferenciasInputDTO input
    ) {
        PreferenciasResponseDTO response = usuarioService.atualizarPreferenciasDoUsuarioAutenticado(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Preferências atualizadas com sucesso"));
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
