package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AdvogadoFormaCobrancaEntity;

public interface AdvogadoFormaCobrancaRepository extends JpaRepository<AdvogadoFormaCobrancaEntity, AdvogadoFormaCobrancaEntity.AdvogadoFormaCobrancaId> {
    List<AdvogadoFormaCobrancaEntity> findByAdvogadoUsuarioId(UUID advogadoId);

    /** Linhas [advogadoId, codigoFormaCobranca] carregadas em lote para o matching. */
    @Query("""
            select afc.id.advogadoId, fc.codigo
            from AdvogadoFormaCobrancaEntity afc
            join afc.formaCobranca fc
            where afc.id.advogadoId in :advogadoIds
            """)
    List<Object[]> findCodigosByAdvogadoIds(@Param("advogadoIds") Collection<UUID> advogadoIds);
}
