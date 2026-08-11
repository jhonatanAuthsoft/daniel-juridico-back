package com.laweact.dto.advogado;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PosGraduacaoInputDTO(
        @NotBlank(message = "O nome do curso de pós-graduação é obrigatório")
        String nomeCurso,

        @NotBlank(message = "A instituição da pós-graduação é obrigatória")
        String instituicao,

        @Min(value = 1950, message = "Ano de formação inválido")
        @Max(value = 2100, message = "Ano de formação inválido")
        Integer anoFormacao
) {}
