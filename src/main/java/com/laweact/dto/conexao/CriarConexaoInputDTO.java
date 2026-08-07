package com.laweact.dto.conexao;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CriarConexaoInputDTO(
        @NotNull(message = "A solicitação é obrigatória")
        UUID solicitacaoId,

        @NotNull(message = "O advogado é obrigatório")
        UUID advogadoId
) {}
