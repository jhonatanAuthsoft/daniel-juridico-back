package com.laweact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.model.entity.TokenRecuperacaoSenhaEntity;

public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenhaEntity, UUID> {

    long countByUsuario_Id(UUID usuarioId);

    Optional<TokenRecuperacaoSenhaEntity> findFirstByUsuario_IdOrderByCreatedAtDesc(UUID usuarioId);

    List<TokenRecuperacaoSenhaEntity> findByUsuario_IdAndUsadoEmIsNullAndInvalidadoEmIsNull(UUID usuarioId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE TokenRecuperacaoSenhaEntity t
            SET t.invalidadoEm = CURRENT_TIMESTAMP
            WHERE t.usuario.id = :usuarioId
              AND t.usadoEm IS NULL
              AND t.invalidadoEm IS NULL
            """)
    int invalidarAtivosPorUsuario(@Param("usuarioId") UUID usuarioId);
}
