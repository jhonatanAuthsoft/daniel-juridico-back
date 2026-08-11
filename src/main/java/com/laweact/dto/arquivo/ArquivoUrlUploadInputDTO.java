package com.laweact.dto.arquivo;

import com.laweact.model.enums.ArquivoFinalidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record ArquivoUrlUploadInputDTO(
        @NotNull(message = "A finalidade é obrigatória")
        ArquivoFinalidade finalidade,

        @NotBlank(message = "O contentType é obrigatório")
        String contentType,

        @Positive(message = "O contentLength deve ser positivo")
        Long contentLength
) {}
