package com.laweact.dto.cliente;

import com.laweact.model.enums.PronomesEnum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AtualizarPerfilPessoalClienteInputDTO(
        @NotNull(message = "Os pronomes são obrigatórios")
        PronomesEnum pronomes,

        String profissao,

        String areaAtuacao,

        @Size(max = 50, message = "O estado civil deve ter no máximo 50 caracteres")
        String estadoCivil,

        @Size(max = 100, message = "A faixa de renda deve ter no máximo 100 caracteres")
        String faixaRenda
) {}
