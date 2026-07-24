package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AdvogadoEspecialidadeEntity;

public interface AdvogadoEspecialidadeRepository extends JpaRepository<AdvogadoEspecialidadeEntity, UUID> {
    List<AdvogadoEspecialidadeEntity> findByAdvogadoUsuarioId(UUID advogadoId);
}
