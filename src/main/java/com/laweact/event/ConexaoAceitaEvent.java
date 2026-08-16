package com.laweact.event;

import java.util.UUID;

public record ConexaoAceitaEvent(
        UUID conexaoId,
        UUID clienteUsuarioId,
        UUID advogadoUsuarioId,
        String nomeAdvogado,
        String tituloSolicitacao
) {}
