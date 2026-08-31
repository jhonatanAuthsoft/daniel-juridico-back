package com.laweact.dto.advogado;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AtualizarGraduacaoAdvogadoInputDTO(
        @NotBlank(message = "A universidade é obrigatória")
        String universidade,

        @NotBlank(message = "O curso é obrigatório")
        String curso,

        @NotNull(message = "O ano de formação é obrigatório")
        @Min(value = 1950, message = "Ano de formação inválido")
        @Max(value = 2100, message = "Ano de formação inválido")
        Integer anoFormacao
) {}
