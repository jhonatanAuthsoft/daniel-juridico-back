package com.laweact.dto.advogado;

import lombok.Builder;

@Builder
public record AdvogadoEnderecoPublicoDTO(
        String bairro,
        String cidade,
        String estado
) {}
