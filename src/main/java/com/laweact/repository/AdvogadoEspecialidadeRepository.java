package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AdvogadoEspecialidadeEntity;

public interface AdvogadoEspecialidadeRepository extends JpaRepository<AdvogadoEspecialidadeEntity, UUID> {
    List<AdvogadoEspecialidadeEntity> findByAdvogadoUsuarioId(UUID advogadoId);

    /** Linhas [advogadoId, codigoEspecialidade, codigoSubespecialidade] carregadas em lote para o matching. */
    @Query("""
            select ae.advogado.usuarioId, e.codigo, s.codigo
            from AdvogadoEspecialidadeEntity ae
            left join ae.especialidade e
            left join ae.subespecialidade s
            where ae.advogado.usuarioId in :advogadoIds
            """)
    List<Object[]> findCodigosByAdvogadoIds(@Param("advogadoIds") Collection<UUID> advogadoIds);
}
