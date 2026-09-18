package com.laweact.dto.advogado;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record AtualizarModalidadesAdvogadoInputDTO(
        @NotEmpty(message = "Informe ao menos uma modalidade de atuação")
        List<String> modalidades
) {}
