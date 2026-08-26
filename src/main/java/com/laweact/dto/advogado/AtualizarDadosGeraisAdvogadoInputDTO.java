package com.laweact.dto.advogado;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AtualizarDadosGeraisAdvogadoInputDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nomeCompleto
) {}
