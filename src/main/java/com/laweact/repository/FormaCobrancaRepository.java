package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.FormaCobrancaEntity;

public interface FormaCobrancaRepository extends JpaRepository<FormaCobrancaEntity, UUID> {
    Optional<FormaCobrancaEntity> findByCodigo(String codigo);
}
