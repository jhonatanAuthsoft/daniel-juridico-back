package com.laweact.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AuditoriaEventoEntity;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEventoEntity, UUID> {
}
