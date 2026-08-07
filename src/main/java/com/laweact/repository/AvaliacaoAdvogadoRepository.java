package com.laweact.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AvaliacaoAdvogadoEntity;

public interface AvaliacaoAdvogadoRepository extends JpaRepository<AvaliacaoAdvogadoEntity, UUID> {

    Page<AvaliacaoAdvogadoEntity> findByAdvogado_UsuarioIdOrderByCreatedAtDesc(
            UUID advogadoId,
            Pageable pageable
    );

    long countByAdvogado_UsuarioId(UUID advogadoId);

    @Query("""
            SELECT COALESCE(AVG(a.nota), 0)
            FROM AvaliacaoAdvogadoEntity a
            WHERE a.advogado.usuarioId = :advogadoId
            """)
    BigDecimal mediaByAdvogadoId(@Param("advogadoId") UUID advogadoId);
}
