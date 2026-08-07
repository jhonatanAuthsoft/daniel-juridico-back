package com.laweact.dto.advogado;

import lombok.Builder;

@Builder
public record AdvogadoOabPublicaDTO(
        String numero,
        String uf,
        boolean principal
) {}
