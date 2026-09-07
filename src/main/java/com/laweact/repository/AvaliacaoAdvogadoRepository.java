package com.laweact.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AvaliacaoAdvogadoEntity;

public interface AvaliacaoAdvogadoRepository extends JpaRepository<AvaliacaoAdvogadoEntity, UUID> {

    @Query(
            value = """
                    SELECT a FROM AvaliacaoAdvogadoEntity a
                    JOIN a.cliente cl
                    JOIN cl.usuario u
                    WHERE a.advogado.usuarioId = :advogadoId
                      AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                    ORDER BY CASE WHEN a.cliente.usuarioId = :usuarioId THEN 0 ELSE 1 END,
                             a.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(a) FROM AvaliacaoAdvogadoEntity a
                    JOIN a.cliente cl
                    JOIN cl.usuario u
                    WHERE a.advogado.usuarioId = :advogadoId
                      AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                    """
    )
    Page<AvaliacaoAdvogadoEntity> findByAdvogadoOrderedWithOwnFirst(
            @Param("advogadoId") UUID advogadoId,
            @Param("usuarioId") UUID usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(a)
            FROM AvaliacaoAdvogadoEntity a
            JOIN a.cliente cl
            JOIN cl.usuario u
            WHERE a.advogado.usuarioId = :advogadoId
              AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            """)
    long countByAdvogado_UsuarioId(@Param("advogadoId") UUID advogadoId);

    boolean existsByAdvogado_UsuarioIdAndCliente_UsuarioId(UUID advogadoId, UUID clienteId);

    Optional<AvaliacaoAdvogadoEntity> findByAdvogado_UsuarioIdAndCliente_UsuarioId(
            UUID advogadoId,
            UUID clienteId
    );

    boolean existsByConexao_Id(UUID conexaoId);

    Optional<AvaliacaoAdvogadoEntity> findByConexao_Id(UUID conexaoId);

    @Query("""
            SELECT COALESCE(AVG(a.nota), 0)
            FROM AvaliacaoAdvogadoEntity a
            JOIN a.cliente cl
            JOIN cl.usuario u
            WHERE a.advogado.usuarioId = :advogadoId
              AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            """)
    BigDecimal mediaByAdvogadoId(@Param("advogadoId") UUID advogadoId);
}
