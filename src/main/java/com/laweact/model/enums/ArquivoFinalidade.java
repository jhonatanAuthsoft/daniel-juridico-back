package com.laweact.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArquivoFinalidade {

    CLIENTE_PERFIL("tmp/clientes/perfil"),
    ADVOGADO_PERFIL("tmp/advogados/perfil"),
    OAB("tmp/advogados/oab");

    private final String prefixo;
}
