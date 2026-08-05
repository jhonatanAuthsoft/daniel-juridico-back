package com.laweact.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.SolicitacaoMatchEntity;

public interface SolicitacaoMatchRepository extends JpaRepository<SolicitacaoMatchEntity, UUID> {

    long countBySolicitacaoId(UUID solicitacaoId);

    @Query("""
            select m from SolicitacaoMatchEntity m
            join fetch m.advogado
            where m.solicitacao.id = :solicitacaoId
            order by m.posicao asc
            """)
    List<SolicitacaoMatchEntity> findRankingBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);
}
