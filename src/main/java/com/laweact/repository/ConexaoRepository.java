package com.laweact.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.ConexaoEntity;
import com.laweact.model.enums.StatusConexaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

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

    @Query(
            value = """
                    select c from ConexaoEntity c
                    join fetch c.advogado a
                    join fetch a.usuario
                    join fetch c.cliente cl
                    join fetch cl.usuario
                    join fetch c.solicitacao s
                    where c.cliente.usuarioId = :clienteId
                      and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                      and c.status in :statuses
                      and (:urgencia is null or s.urgencia = :urgencia)
                      and (
                        :busca = ''
                        or lower(a.nomeCompleto) like lower(concat('%', :busca, '%'))
                        or lower(s.titulo) like lower(concat('%', :busca, '%'))
                        or lower(s.descricao) like lower(concat('%', :busca, '%'))
                        or lower(s.cidade) like lower(concat('%', :busca, '%'))
                      )
                    order by c.createdAt desc
                    """,
            countQuery = """
                    select count(c) from ConexaoEntity c
                    join c.advogado a
                    join a.usuario
                    join c.solicitacao s
                    where c.cliente.usuarioId = :clienteId
                      and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                      and c.status in :statuses
                      and (:urgencia is null or s.urgencia = :urgencia)
                      and (
                        :busca = ''
                        or lower(a.nomeCompleto) like lower(concat('%', :busca, '%'))
                        or lower(s.titulo) like lower(concat('%', :busca, '%'))
                        or lower(s.descricao) like lower(concat('%', :busca, '%'))
                        or lower(s.cidade) like lower(concat('%', :busca, '%'))
                      )
                    """
    )
    Page<ConexaoEntity> findForCliente(
            @Param("clienteId") UUID clienteId,
            @Param("statuses") Collection<StatusConexaoEnum> statuses,
            @Param("urgencia") UrgenciaSolicitacaoEnum urgencia,
            @Param("busca") String busca,
            Pageable pageable
    );

    /**
     * Inbox do advogado. Emergências primeiro, depois urgentes, médias e
     * "tenho tempo"; dentro de cada grau, as mais recentes na frente.
     *
     * <p>A coluna {@code urgencia} é {@code EnumType.STRING}, então ordenar
     * pela coluna sairia em ordem alfabética (MEDIO antes de URGENTE). O
     * {@code case} abaixo materializa a ordem de prioridade real.
     */
    @Query(
            value = """
                    select c from ConexaoEntity c
                    join fetch c.advogado a
                    join fetch a.usuario
                    join fetch c.cliente cl
                    join fetch cl.usuario
                    join fetch c.solicitacao s
                    where c.advogado.usuarioId = :advogadoId
                      and cl.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                      and c.status in :statuses
                      and (:urgencia is null or s.urgencia = :urgencia)
                      and (
                        :busca = ''
                        or lower(cl.nomeCompleto) like lower(concat('%', :busca, '%'))
                        or lower(s.titulo) like lower(concat('%', :busca, '%'))
                        or lower(s.descricao) like lower(concat('%', :busca, '%'))
                        or lower(s.cidade) like lower(concat('%', :busca, '%'))
                      )
                    order by
                      case
                        when s.urgencia = com.laweact.model.enums.UrgenciaSolicitacaoEnum.EMERGENCIA then 0
                        when s.urgencia = com.laweact.model.enums.UrgenciaSolicitacaoEnum.URGENTE then 1
                        when s.urgencia = com.laweact.model.enums.UrgenciaSolicitacaoEnum.MEDIO then 2
                        else 3
                      end asc,
                      c.createdAt desc
                    """,
            countQuery = """
                    select count(c) from ConexaoEntity c
                    join c.cliente cl
                    join cl.usuario
                    join c.solicitacao s
                    where c.advogado.usuarioId = :advogadoId
                      and cl.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
                      and c.status in :statuses
                      and (:urgencia is null or s.urgencia = :urgencia)
                      and (
                        :busca = ''
                        or lower(cl.nomeCompleto) like lower(concat('%', :busca, '%'))
                        or lower(s.titulo) like lower(concat('%', :busca, '%'))
                        or lower(s.descricao) like lower(concat('%', :busca, '%'))
                        or lower(s.cidade) like lower(concat('%', :busca, '%'))
                      )
                    """
    )
    Page<ConexaoEntity> findForAdvogado(
            @Param("advogadoId") UUID advogadoId,
            @Param("statuses") Collection<StatusConexaoEnum> statuses,
            @Param("urgencia") UrgenciaSolicitacaoEnum urgencia,
            @Param("busca") String busca,
            Pageable pageable
    );

    /** Contagem global por urgência, ignorando filtro de urgência e busca. */
    @Query("""
            select s.urgencia, count(c)
            from ConexaoEntity c
            join c.solicitacao s
            join c.cliente cl
            join cl.usuario
            where c.advogado.usuarioId = :advogadoId
              and cl.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
              and c.status in :statuses
            group by s.urgencia
            """)
    List<Object[]> countGroupedByUrgenciaForAdvogado(
            @Param("advogadoId") UUID advogadoId,
            @Param("statuses") Collection<StatusConexaoEnum> statuses
    );

    /** Contagem global por urgência, ignorando filtro de urgência e busca. */
    @Query("""
            select s.urgencia, count(c)
            from ConexaoEntity c
            join c.solicitacao s
            join c.advogado a
            join a.usuario
            where c.cliente.usuarioId = :clienteId
              and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
              and c.status in :statuses
            group by s.urgencia
            """)
    List<Object[]> countGroupedByUrgenciaForCliente(
            @Param("clienteId") UUID clienteId,
            @Param("statuses") Collection<StatusConexaoEnum> statuses
    );

    /** Contagem global por status, ignorando filtro de status, urgência e busca. */
    @Query("""
            select c.status, count(c)
            from ConexaoEntity c
            join c.cliente cl
            join cl.usuario
            where c.advogado.usuarioId = :advogadoId
              and cl.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            group by c.status
            """)
    List<Object[]> countGroupedByStatusForAdvogado(@Param("advogadoId") UUID advogadoId);

    /** Contagem global por status, ignorando filtro de status, urgência e busca. */
    @Query("""
            select c.status, count(c)
            from ConexaoEntity c
            join c.advogado a
            join a.usuario
            where c.cliente.usuarioId = :clienteId
              and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            group by c.status
            """)
    List<Object[]> countGroupedByStatusForCliente(@Param("clienteId") UUID clienteId);

    List<ConexaoEntity> findByCliente_UsuarioIdOrAdvogado_UsuarioId(UUID clienteId, UUID advogadoId);

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao
            where c.solicitacao.id = :solicitacaoId
              and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
            order by c.createdAt desc
            """)
    List<ConexaoEntity> findBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);

    @Query("SELECT c.id FROM ConexaoEntity c WHERE c.solicitacao.id = :solicitacaoId")
    List<UUID> findIdsBySolicitacaoId(@Param("solicitacaoId") UUID solicitacaoId);

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

    List<ConexaoEntity> findByCliente_UsuarioIdAndAdvogado_UsuarioIdAndStatusOrderByCreatedAtAsc(
            UUID clienteId,
            UUID advogadoId,
            StatusConexaoEnum status
    );

    @Query("""
            select c from ConexaoEntity c
            join fetch c.advogado a
            join fetch a.usuario
            join fetch c.cliente cl
            join fetch cl.usuario
            join fetch c.solicitacao s
            where c.status = :status
              and a.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
              and cl.usuario.status = com.laweact.model.enums.StatusUsuarioEnum.ATIVO
              and s.urgencia in :urgencias
              and c.visualizadaEm is null
              and (
                    (c.ultimoLembreteInsistenteEm is null and c.createdAt <= :limite)
                 or (c.ultimoLembreteInsistenteEm is not null and c.ultimoLembreteInsistenteEm <= :limite)
              )
            order by c.createdAt asc
            """)
    List<ConexaoEntity> findPendentesParaLembreteInsistente(
            @Param("status") StatusConexaoEnum status,
            @Param("urgencias") Collection<UrgenciaSolicitacaoEnum> urgencias,
            @Param("limite") LocalDateTime limite
    );
}
