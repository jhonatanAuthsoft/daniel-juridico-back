package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.EspecialidadeEntity;

public interface EspecialidadeRepository extends JpaRepository<EspecialidadeEntity, UUID> {
    Optional<EspecialidadeEntity> findByCodigo(String codigo);
}
