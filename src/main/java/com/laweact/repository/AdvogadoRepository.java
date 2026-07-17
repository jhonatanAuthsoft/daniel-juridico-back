package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AdvogadoEntity;

public interface AdvogadoRepository extends JpaRepository<AdvogadoEntity, UUID> {

    Optional<AdvogadoEntity> findByUsuarioId(UUID usuarioId);

    boolean existsByCpf(String cpf);
}
