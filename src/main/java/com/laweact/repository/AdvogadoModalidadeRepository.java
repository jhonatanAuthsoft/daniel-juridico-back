package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AdvogadoModalidadeEntity;

public interface AdvogadoModalidadeRepository extends JpaRepository<AdvogadoModalidadeEntity, AdvogadoModalidadeEntity.AdvogadoModalidadeId> {
    List<AdvogadoModalidadeEntity> findByAdvogadoUsuarioId(UUID advogadoId);
}
