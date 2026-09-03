package com.laweact.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AssinaturaEventoEntity;

public interface AssinaturaEventoRepository extends JpaRepository<AssinaturaEventoEntity, UUID> {

    boolean existsByEventoExternoId(String eventoExternoId);
}
