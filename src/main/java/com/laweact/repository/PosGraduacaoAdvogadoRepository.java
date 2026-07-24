package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.PosGraduacaoAdvogadoEntity;

public interface PosGraduacaoAdvogadoRepository extends JpaRepository<PosGraduacaoAdvogadoEntity, UUID> {
    List<PosGraduacaoAdvogadoEntity> findByAdvogadoUsuarioId(UUID advogadoId);
}
