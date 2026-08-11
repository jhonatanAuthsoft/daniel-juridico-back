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

        /** Keys S3 das imagens da carteira (N fotos; limite de produto no front depois). */
        @Size(max = 20, message = "São permitidas no máximo 20 fotos por OAB")
        List<String> fotosUrls
) {}
