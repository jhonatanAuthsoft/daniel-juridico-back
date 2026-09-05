package com.laweact.dto.avaliacao;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CriarAvaliacaoInputDTO(
        @NotNull(message = "A nota é obrigatória")
        @DecimalMin(value = "0.5", message = "A nota mínima é 0.5")
        @DecimalMax(value = "5.0", message = "A nota máxima é 5.0")
        BigDecimal nota,

        @Size(max = 800, message = "O comentário deve ter no máximo 800 caracteres")
        String comentario
) {}
