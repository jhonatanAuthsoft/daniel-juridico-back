package com.laweact.dto.advogado;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record AtualizarFormasCobrancaAdvogadoInputDTO(
        @NotEmpty(message = "Informe ao menos uma forma de cobrança")
        List<String> formasCobranca
) {}
