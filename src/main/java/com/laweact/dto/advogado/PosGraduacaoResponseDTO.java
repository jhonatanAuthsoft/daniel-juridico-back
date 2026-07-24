package com.laweact.dto.advogado;

import java.util.UUID;

import lombok.Builder;

@Builder
public record PosGraduacaoResponseDTO(
        UUID id,
        String nomeCurso,
        String instituicao,
        Integer anoFormacao
) {}
