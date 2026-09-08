package com.laweact.service;

import java.util.UUID;

import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.dto.assinatura.ValidarAssinaturaInputDTO;
import com.laweact.dto.job.AssinaturaReconciliacaoJobResultDTO;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;

public interface AssinaturaService {

    AssinaturaEntity criarAssinaturaPendenteParaAdvogado(UsuarioEntity usuario);

    AssinaturaResponseDTO obterMinhaAssinatura();

    AssinaturaResponseDTO validarCompra(ValidarAssinaturaInputDTO input);

    AssinaturaResponseDTO obterAssinaturaDoUsuario(UsuarioEntity usuario);

    AssinaturaReconciliacaoJobResultDTO reconciliar();

    void bloquearAssinatura(UUID usuarioId);

    void expirarAssinatura(UUID usuarioId);

    void renovarAssinatura(UUID usuarioId);

    void resetAssinatura(UUID usuarioId);
}
