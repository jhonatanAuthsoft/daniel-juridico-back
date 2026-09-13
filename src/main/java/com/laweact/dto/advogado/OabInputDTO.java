package com.laweact.dto.advogado;

import java.time.LocalDate;
import java.util.List;

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

        @NotNull(message = "As fotos de frente e verso da carteira OAB são obrigatórias")
        @Size(min = 2, max = 20, message = "Envie as fotos de frente e verso da carteira OAB")
        List<String> fotosUrls
) {}
