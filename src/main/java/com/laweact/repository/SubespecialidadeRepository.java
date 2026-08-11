package com.laweact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.SubespecialidadeEntity;

public interface SubespecialidadeRepository extends JpaRepository<SubespecialidadeEntity, UUID> {
    Optional<SubespecialidadeEntity> findByEspecialidadeIdAndCodigo(UUID especialidadeId, String codigo);

    List<SubespecialidadeEntity> findByEspecialidadeIdOrderByNomeAsc(UUID especialidadeId);
}
