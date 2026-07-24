package com.laweact.dto.advogado;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record OabInputDTO(
        @NotBlank(message = "O número da OAB é obrigatório")
        String numero,

        @NotBlank(message = "A UF da OAB é obrigatória")
        @Size(min = 2, max = 2, message = "A UF deve ter 2 letras")
        String uf,

        @NotNull(message = "A data de expedição da OAB é obrigatória")
        @PastOrPresent(message = "A data de expedição deve ser no passado ou presente")
        LocalDate dataExpedicao,

        String fotoFrenteUrl,
        String fotoVersoUrl
) {}
