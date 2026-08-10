package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.SolicitacaoEntity;
import com.laweact.model.enums.StatusSolicitacaoEnum;

public interface SolicitacaoRepository extends JpaRepository<SolicitacaoEntity, UUID> {

    @Query(
            value = """
                    SELECT s FROM SolicitacaoEntity s
                    WHERE s.cliente.usuarioId = :usuarioId
                      AND (:status IS NULL OR s.status = :status)
                      AND (
                        :busca = ''
                        OR LOWER(s.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                        OR LOWER(s.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                      )
                    ORDER BY s.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(s) FROM SolicitacaoEntity s
                    WHERE s.cliente.usuarioId = :usuarioId
                      AND (:status IS NULL OR s.status = :status)
                      AND (
                        :busca = ''
                        OR LOWER(s.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                        OR LOWER(s.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                      )
                    """
    )
    Page<SolicitacaoEntity> findForCliente(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusSolicitacaoEnum status,
            @Param("busca") String busca,
            Pageable pageable
    );

    @Query("""
            SELECT s.status, COUNT(s)
            FROM SolicitacaoEntity s
            WHERE s.cliente.usuarioId = :usuarioId
            GROUP BY s.status
            """)
    List<Object[]> countGroupedByStatusForCliente(@Param("usuarioId") UUID usuarioId);
}
