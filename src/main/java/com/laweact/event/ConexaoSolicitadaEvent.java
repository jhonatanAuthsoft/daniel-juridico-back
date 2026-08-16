package com.laweact.event;

import java.util.UUID;

public record ConexaoSolicitadaEvent(
        UUID conexaoId,
        UUID clienteUsuarioId,
        UUID advogadoUsuarioId,
        String nomeCliente,
        String tituloSolicitacao
) {}
