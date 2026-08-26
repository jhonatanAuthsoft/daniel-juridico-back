package com.laweact.dto.advogado;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AtualizarDocumentacaoAdvogadoInputDTO(
        @NotNull(message = "A OAB principal é obrigatória")
        @Valid
        OabInputDTO oabPrincipal,

        @Size(max = 5, message = "São permitidas no máximo 5 OABs suplementares")
        @Valid
        List<OabInputDTO> oabsSuplementares
) {}
