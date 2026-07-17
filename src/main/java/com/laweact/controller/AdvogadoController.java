package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.CadastrarAdvogadoResponseDTO;
import com.laweact.service.AdvogadoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/advogados")
@Tag(name = "Advogados", description = "Cadastro e perfil de advogados")
public class AdvogadoController {

    private final AdvogadoService advogadoService;

    @PostMapping("/cadastrar")
    @Operation(
            summary = "Cadastrar advogado",
            description = "Cria usuário + perfil de advogado + endereço + OAB(s) + áreas de atuação e retorna JWT"
    )
    public ResponseEntity<CadastrarAdvogadoResponseDTO> cadastrar(
            @Valid @RequestBody CadastrarAdvogadoInputDTO input
    ) {
        CadastrarAdvogadoResponseDTO response = advogadoService.cadastrar(input);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
