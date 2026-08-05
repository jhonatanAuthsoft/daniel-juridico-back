package com.laweact.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.SolicitacaoEntity;

public interface SolicitacaoRepository extends JpaRepository<SolicitacaoEntity, UUID> {
}
