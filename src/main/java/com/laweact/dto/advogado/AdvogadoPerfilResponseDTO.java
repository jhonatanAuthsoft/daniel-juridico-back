package com.laweact.dto.advogado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PronomeTratamentoEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AdvogadoPerfilResponseDTO(
        @NotNull UUID usuarioId,
        @NotBlank String nomeCompleto,
        String nomeSocial,
        @NotBlank String rg,
        @NotBlank String rgOrgaoEmissor,
        @NotBlank String rgUf,
        @NotBlank String cpf,
        String nomePai,
        @NotBlank String nomeMae,
        @NotNull PronomeTratamentoEnum pronomeTratamento,
        String fotoUrl,
        @NotBlank String universidade,
        @NotBlank String curso,
        @NotNull Integer anoFormacao,
        @NotNull LocalDate atuacaoDesde,
        String biografia,
        @NotNull DisponibilidadeAdvogadoEnum disponibilidade,
        @NotNull StatusVerificacaoEnum statusVerificacao,
        @NotNull BigDecimal mediaAvaliacoes,
        @NotNull Integer totalAvaliacoes
) {}
