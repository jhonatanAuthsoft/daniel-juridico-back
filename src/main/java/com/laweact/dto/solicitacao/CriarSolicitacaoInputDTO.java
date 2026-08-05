package com.laweact.dto.solicitacao;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CriarSolicitacaoInputDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String titulo,

        @NotNull(message = "A modalidade é obrigatória")
        ModalidadeSolicitacaoEnum modalidade,

        @NotBlank(message = "A especialidade é obrigatória")
        String especialidadeCodigo,

        String subespecialidadeCodigo,

        @NotBlank(message = "A UF é obrigatória")
        @Size(min = 2, max = 2, message = "A UF deve ter 2 letras")
        String uf,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 120, message = "A cidade deve ter no máximo 120 caracteres")
        String cidade,

        @NotNull(message = "A urgência é obrigatória")
        UrgenciaSolicitacaoEnum urgencia,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 800, message = "A descrição deve ter no máximo 800 caracteres")
        String descricao,

        FormaCobrancaSolicitacaoEnum formaCobranca,

        @Min(value = 0, message = "A experiência mínima não pode ser negativa")
        @Max(value = 600, message = "A experiência mínima é inválida")
        Integer experienciaMinimaMeses
) {}
