package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.enums.StatusConexaoEnum;

public interface ConexaoRepository extends JpaRepository<ConexaoEntity, UUID> {

    Optional<ConexaoEntity> findBySolicitacao_IdAndAdvogado_UsuarioId(UUID solicitacaoId, UUID advogadoId);

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao
            where c.id = :id
            """)
    Optional<ConexaoEntity> findDetailedById(@Param("id") UUID id);

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao
            where c.cliente.usuarioId = :clienteId
              and (:status is null or c.status = :status)
            order by c.createdAt desc
            """)
    List<ConexaoEntity> findByCliente(
            @Param("clienteId") UUID clienteId,
            @Param("status") StatusConexaoEnum status
    );

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao
            where c.advogado.usuarioId = :advogadoId
              and (:status is null or c.status = :status)
            order by c.createdAt desc
            """)
    List<ConexaoEntity> findByAdvogado(
            @Param("advogadoId") UUID advogadoId,
            @Param("status") StatusConexaoEnum status
    );

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao
            where c.solicitacao.id = :solicitacaoId
            order by c.createdAt desc
            """)
    List<ConexaoEntity> findBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);

    @Query("""
            SELECT c.solicitacao.id, COUNT(c)
            FROM ConexaoEntity c
            WHERE c.solicitacao.id IN :solicitacaoIds
              AND c.status = :status
            GROUP BY c.solicitacao.id
            """)
    List<Object[]> countGroupedBySolicitacaoIdsAndStatus(
            @Param("solicitacaoIds") Collection<UUID> solicitacaoIds,
            @Param("status") StatusConexaoEnum status
    );

    boolean existsByCliente_UsuarioIdAndAdvogado_UsuarioIdAndStatus(
            UUID clienteId,
            UUID advogadoId,
            StatusConexaoEnum status
    );
}
