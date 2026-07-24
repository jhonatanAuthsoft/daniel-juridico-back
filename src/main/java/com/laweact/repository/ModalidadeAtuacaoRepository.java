package com.laweact.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.ModalidadeAtuacaoEntity;

public interface ModalidadeAtuacaoRepository extends JpaRepository<ModalidadeAtuacaoEntity, UUID> {
    Optional<ModalidadeAtuacaoEntity> findByCodigo(String codigo);
}
