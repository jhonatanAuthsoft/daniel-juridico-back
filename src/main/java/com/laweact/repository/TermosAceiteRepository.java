package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.TermosAceiteEntity;

public interface TermosAceiteRepository extends JpaRepository<TermosAceiteEntity, UUID> {

    boolean existsByUsuario_Id(UUID usuarioId);

    boolean existsByUsuario_IdAndVersao(UUID usuarioId, String versao);

    Optional<TermosAceiteEntity> findFirstByUsuario_IdOrderByAceitoEmDesc(UUID usuarioId);
}
