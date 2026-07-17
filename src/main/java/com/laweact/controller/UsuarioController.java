package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.CadastrarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
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
    public ResponseEntity<LoginUsuarioResponseDTO> login(@Valid @RequestBody LoginUsuarioInputDTO loginUsuarioDTO) {
        LoginUsuarioResponseDTO response = usuarioService.login(loginUsuarioDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastrar usuário", description = "Cadastro público para testes locais")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody CadastrarUsuarioInputDTO input) {
        UsuarioResponseDTO response = usuarioService.cadastrar(input);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o token JWT atual")
    public ResponseEntity<Boolean> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomError("Cabeçalho de autorização ausente ou inválido", HttpStatus.BAD_REQUEST);
        }
        String token = authorizationHeader.substring(7);
        usuarioService.logout(token);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado", description = "Retorna o usuário logado")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(usuarioService.obterUsuarioAutenticado());
    }
}
