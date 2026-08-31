package com.laweact.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AtualizarDadosGeraisClienteInputDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nomeCompleto
) {}
