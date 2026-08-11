package com.laweact.dto.advogado;

import java.time.LocalDate;
import java.util.List;
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
        @NotNull LocalDate dataExpedicao,
        @NotNull Boolean principal,
        List<String> fotosUrls,
        @NotNull StatusVerificacaoEnum statusValidacao
) {}
