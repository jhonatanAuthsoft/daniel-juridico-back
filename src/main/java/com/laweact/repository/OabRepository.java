package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.OabEntity;

public interface OabRepository extends JpaRepository<OabEntity, UUID> {

    boolean existsByNumeroAndUf(String numero, String uf);

    boolean existsByNumeroAndUfAndAdvogadoUsuarioIdNot(String numero, String uf, UUID advogadoId);

    List<OabEntity> findByAdvogadoUsuarioId(UUID advogadoId);
}
