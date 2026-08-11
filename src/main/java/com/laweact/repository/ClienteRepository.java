package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.ClienteEntity;

public interface ClienteRepository extends JpaRepository<ClienteEntity, UUID> {

    Optional<ClienteEntity> findByUsuarioId(UUID usuarioId);

    boolean existsByNumeroDocumento(String numeroDocumento);
}
