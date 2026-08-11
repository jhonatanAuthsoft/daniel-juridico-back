package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AreaAtuacaoAdvogadoEntity;

public interface AreaAtuacaoAdvogadoRepository extends JpaRepository<AreaAtuacaoAdvogadoEntity, UUID> {

    List<AreaAtuacaoAdvogadoEntity> findByAdvogadoUsuarioId(UUID advogadoId);

    /** Linhas [advogadoId, estado, cidade] carregadas em lote para o matching. */
    @Query("""
            select a.advogado.usuarioId, a.estado, a.cidade
            from AreaAtuacaoAdvogadoEntity a
            where a.advogado.usuarioId in :advogadoIds
            """)
    List<Object[]> findAreasByAdvogadoIds(@Param("advogadoIds") Collection<UUID> advogadoIds);
}
