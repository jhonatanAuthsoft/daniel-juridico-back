package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AdvogadoFormaCobrancaEntity;

public interface AdvogadoFormaCobrancaRepository extends JpaRepository<AdvogadoFormaCobrancaEntity, AdvogadoFormaCobrancaEntity.AdvogadoFormaCobrancaId> {
    List<AdvogadoFormaCobrancaEntity> findByAdvogadoUsuarioId(UUID advogadoId);
}
