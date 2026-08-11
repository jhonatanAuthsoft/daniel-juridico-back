package com.laweact.dto.advogado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AreaAtuacaoInputDTO(
        @NotBlank(message = "O estado da área de atuação é obrigatório")
        @Size(min = 2, max = 2, message = "O estado deve ter 2 letras (UF)")
        String estado,

        @NotBlank(message = "A cidade da área de atuação é obrigatória")
        String cidade
) {}
