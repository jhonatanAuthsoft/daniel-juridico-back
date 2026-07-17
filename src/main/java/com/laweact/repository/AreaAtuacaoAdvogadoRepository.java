package com.laweact.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;

public interface AreaAtuacaoAdvogadoRepository extends JpaRepository<AreaAtuacaoAdvogadoEntity, UUID> {
}
