package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.SolicitacaoMatchEntity;

public interface SolicitacaoMatchRepository extends JpaRepository<SolicitacaoMatchEntity, UUID> {

    boolean existsBySolicitacao_IdAndAdvogado_UsuarioId(UUID solicitacaoId, UUID advogadoId);

    @Query("""
            SELECT m.solicitacao.id, COUNT(m)
            FROM SolicitacaoMatchEntity m
            JOIN m.advogado a
            JOIN a.usuario u
            WHERE m.solicitacao.id IN :solicitacaoIds
              AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            GROUP BY m.solicitacao.id
            """)
    List<Object[]> countGroupedBySolicitacaoIds(@Param("solicitacaoIds") Collection<UUID> solicitacaoIds);

    @Query("""
            select m from SolicitacaoMatchEntity m
            join fetch m.advogado a
            join fetch a.usuario u
            where m.solicitacao.id = :solicitacaoId
              and u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            order by m.posicao asc
            """)
    List<SolicitacaoMatchEntity> findRankingBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);

    @Query("""
            SELECT COUNT(m)
            FROM SolicitacaoMatchEntity m
            JOIN m.advogado a
            JOIN a.usuario u
            WHERE m.solicitacao.id = :solicitacaoId
              AND u.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            """)
    long countBySolicitacao_Id(@Param("solicitacaoId") UUID solicitacaoId);
}
