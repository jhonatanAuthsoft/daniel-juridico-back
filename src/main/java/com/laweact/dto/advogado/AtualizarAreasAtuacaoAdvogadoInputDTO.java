package com.laweact.dto.advogado;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record AtualizarAreasAtuacaoAdvogadoInputDTO(
        @NotEmpty(message = "Informe ao menos uma área de atuação")
        @Valid
        List<AreaAtuacaoInputDTO> areasAtuacao
) {}
