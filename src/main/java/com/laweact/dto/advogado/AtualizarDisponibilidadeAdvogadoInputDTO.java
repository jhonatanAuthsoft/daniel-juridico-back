package com.laweact.dto.advogado;

import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AtualizarDisponibilidadeAdvogadoInputDTO(
        @NotNull(message = "A disponibilidade é obrigatória")
        DisponibilidadeAdvogadoEnum disponibilidade
) {}
