package com.laweact.dto.advogado;

import com.laweact.model.enums.PronomeTratamentoEnum;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AtualizarBiografiaAdvogadoInputDTO(
        @NotNull(message = "O pronome de tratamento é obrigatório")
        PronomeTratamentoEnum pronomeTratamento,

        @Size(max = 800, message = "A biografia deve ter no máximo 800 caracteres")
        String biografia
) {}
