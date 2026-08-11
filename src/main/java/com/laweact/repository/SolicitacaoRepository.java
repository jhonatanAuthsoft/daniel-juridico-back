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

    Page<SolicitacaoEntity> findByCliente_UsuarioIdOrderByCreatedAtDesc(UUID usuarioId, Pageable pageable);

    Page<SolicitacaoEntity> findByCliente_UsuarioIdAndStatusOrderByCreatedAtDesc(
            UUID usuarioId,
            StatusSolicitacaoEnum status,
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
