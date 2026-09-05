package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AdvogadoModalidadeEntity;

public interface AdvogadoModalidadeRepository extends JpaRepository<AdvogadoModalidadeEntity, AdvogadoModalidadeEntity.AdvogadoModalidadeId> {
    List<AdvogadoModalidadeEntity> findByAdvogadoUsuarioId(UUID advogadoId);

    /** Linhas [advogadoId, codigoModalidade] carregadas em lote para o matching. */
    @Query("""
            select am.id.advogadoId, m.codigo
            from AdvogadoModalidadeEntity am
            join am.modalidade m
            where am.id.advogadoId in :advogadoIds
            """)
    List<Object[]> findCodigosByAdvogadoIds(@Param("advogadoIds") Collection<UUID> advogadoIds);

    /** Linhas [advogadoId, codigo, nome] para o card de advogados compatíveis. */
    @Query("""
            select am.id.advogadoId, m.codigo, m.nome
            from AdvogadoModalidadeEntity am
            join am.modalidade m
            where am.id.advogadoId in :advogadoIds
            """)
    List<Object[]> findCodigoENomeByAdvogadoIds(@Param("advogadoIds") Collection<UUID> advogadoIds);
}
