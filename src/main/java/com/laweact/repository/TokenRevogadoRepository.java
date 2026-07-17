package com.laweact.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laweact.model.entity.TokenRevogadoEntity;

@Repository
public interface TokenRevogadoRepository extends JpaRepository<TokenRevogadoEntity, UUID> {
    boolean existsByToken(String token);
}
