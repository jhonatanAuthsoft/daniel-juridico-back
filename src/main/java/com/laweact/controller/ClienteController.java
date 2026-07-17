package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
import com.laweact.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Cadastro e perfil de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping("/cadastrar")
    @Operation(
            summary = "Cadastrar cliente",
            description = "Cria usuário (auth) + perfil de cliente + endereço em uma única chamada e retorna JWT"
    )
    public ResponseEntity<CadastrarClienteResponseDTO> cadastrar(
            @Valid @RequestBody CadastrarClienteInputDTO input
    ) {
        CadastrarClienteResponseDTO response = clienteService.cadastrar(input);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
