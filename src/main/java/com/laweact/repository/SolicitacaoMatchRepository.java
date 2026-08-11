package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.SolicitacaoMatchEntity;

public interface SolicitacaoMatchRepository extends JpaRepository<SolicitacaoMatchEntity, UUID> {

    long countBySolicitacao_Id(UUID solicitacaoId);

    boolean existsBySolicitacao_IdAndAdvogado_UsuarioId(UUID solicitacaoId, UUID advogadoId);

    @Query("""
            SELECT m.solicitacao.id, COUNT(m)
            FROM SolicitacaoMatchEntity m
            WHERE m.solicitacao.id IN :solicitacaoIds
            GROUP BY m.solicitacao.id
            """)
    List<Object[]> countGroupedBySolicitacaoIds(@Param("solicitacaoIds") Collection<UUID> solicitacaoIds);

    @Query("""
            select m from SolicitacaoMatchEntity m
            join fetch m.advogado
            where m.solicitacao.id = :solicitacaoId
            order by m.posicao asc
            """)
    List<SolicitacaoMatchEntity> findRankingBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);
}
