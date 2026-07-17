package com.laweact.dto.advogado;

import java.util.UUID;

import com.laweact.model.enums.StatusVerificacaoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OabResponseDTO(
        @NotNull UUID id,
        @NotBlank String numero,
        @NotBlank String uf,
        @NotNull Boolean principal,
        String fotoFrenteUrl,
        String fotoVersoUrl,
        @NotNull StatusVerificacaoEnum statusValidacao
) {}
