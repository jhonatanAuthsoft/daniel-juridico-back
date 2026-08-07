package com.laweact.dto.advogado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PronomeTratamentoEnum;

import lombok.Builder;

/**
 * Perfil público do advogado para o cliente (sem PII sensível: CPF, RG, telefone, e-mail, fotos OAB).
 * Alinha aos campos da tela "Visualizar perfil" no app.
 */
@Builder
public record AdvogadoPerfilPublicoResponseDTO(
        UUID id,
        String nome,
        String nomeCompleto,
        String nomeSocial,
        PronomeTratamentoEnum pronomeTratamento,
        String fotoUrl,
        String biografia,
        DisponibilidadeAdvogadoEnum disponibilidade,
        BigDecimal mediaAvaliacoes,
        Integer totalAvaliacoes,
        String universidade,
        String curso,
        Integer anoFormacao,
        LocalDate atuacaoDesde,
        Integer anosExperiencia,
        AdvogadoEnderecoPublicoDTO endereco,
        AdvogadoOabPublicaDTO oabPrincipal,
        List<AdvogadoOabPublicaDTO> oabsSuplementares,
        List<CatalogoItemResponseDTO> modalidades,
        List<CatalogoItemResponseDTO> especialidades,
        List<CatalogoItemResponseDTO> subespecialidades,
        List<CatalogoItemResponseDTO> formasCobranca,
        List<AreaAtuacaoResponseDTO> areasAtuacao,
        List<PosGraduacaoResponseDTO> posGraduacoes
) {}
