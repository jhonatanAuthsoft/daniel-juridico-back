package com.laweact.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.LogAcessoTelaEntity;

import java.util.UUID;

public interface LogAcessoTelaRepository extends JpaRepository<LogAcessoTelaEntity, UUID> {
}
