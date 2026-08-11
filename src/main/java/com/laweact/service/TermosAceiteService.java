package com.laweact.service;

import com.laweact.dto.usuario.AceitarTermosInputDTO;
import com.laweact.dto.usuario.AceitarTermosResponseDTO;
import com.laweact.model.entity.TermosAceiteEntity;

import java.util.Optional;
import java.util.UUID;

public interface TermosAceiteService {

    AceitarTermosResponseDTO aceitar(AceitarTermosInputDTO input);

    boolean possuiAceite(UUID usuarioId);

    Optional<TermosAceiteEntity> obterUltimoAceite(UUID usuarioId);
}
