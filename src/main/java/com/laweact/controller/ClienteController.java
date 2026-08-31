package com.laweact.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.dto.cliente.AtualizarDadosGeraisClienteInputDTO;
import com.laweact.dto.cliente.AtualizarEnderecoClienteInputDTO;
import com.laweact.dto.cliente.AtualizarPerfilPessoalClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.dto.cliente.CadastrarClienteResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.shared.ApiResponse;
import com.laweact.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    public ResponseEntity<ApiResponse<CadastrarClienteResponseDTO>> cadastrar(
            @Valid @RequestBody CadastrarClienteInputDTO input
    ) {
        CadastrarClienteResponseDTO response = clienteService.cadastrar(input);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Cliente cadastrado com sucesso"),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/me/dados-gerais")
    @Operation(
            summary = "Atualizar dados gerais",
            description = "Atualiza o nome do cliente autenticado. CPF/CNPJ, RG e e-mail não são editáveis."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ClienteDetalheResponseDTO>> atualizarDadosGerais(
            @Valid @RequestBody AtualizarDadosGeraisClienteInputDTO input
    ) {
        ClienteDetalheResponseDTO response = clienteService.atualizarDadosGerais(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Dados gerais atualizados com sucesso"));
    }

    @PatchMapping("/me/endereco")
    @Operation(
            summary = "Atualizar endereço",
            description = "Atualiza o endereço do cliente autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ClienteDetalheResponseDTO>> atualizarEndereco(
            @Valid @RequestBody AtualizarEnderecoClienteInputDTO input
    ) {
        ClienteDetalheResponseDTO response = clienteService.atualizarEndereco(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Endereço atualizado com sucesso"));
    }

    @PatchMapping("/me/perfil-pessoal")
    @Operation(
            summary = "Atualizar perfil pessoal",
            description = "Atualiza pronomes, profissão/área de atuação, estado civil e faixa de renda do cliente autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ClienteDetalheResponseDTO>> atualizarPerfilPessoal(
            @Valid @RequestBody AtualizarPerfilPessoalClienteInputDTO input
    ) {
        ClienteDetalheResponseDTO response = clienteService.atualizarPerfilPessoal(input);
        return ResponseEntity.ok(ApiResponse.success(response, "Perfil pessoal atualizado com sucesso"));
    }
}
