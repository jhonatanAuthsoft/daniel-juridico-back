package com.laweact.dto.cliente;

import java.time.LocalDate;
import java.util.UUID;

import com.laweact.model.enums.PronomesEnum;
import com.laweact.model.enums.TipoDocumentoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ClientePerfilResponseDTO(
        @NotNull UUID usuarioId,
        @NotBlank String nomeCompleto,
        String razaoSocial,
        String areaAtuacao,
        String profissao,
        @NotNull TipoDocumentoEnum tipoDocumento,
        @NotBlank String numeroDocumento,
        String rg,
        String rgOrgaoEmissor,
        String rgUf,
        LocalDate dataNascimento,
        @NotNull PronomesEnum pronomes,
        String fotoUrl,
        String faixaRenda,
        String estadoCivil
) {}
