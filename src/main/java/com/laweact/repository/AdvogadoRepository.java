package com.laweact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

public interface AdvogadoRepository extends JpaRepository<AdvogadoEntity, UUID> {

    Optional<AdvogadoEntity> findByUsuarioId(UUID usuarioId);

    boolean existsByCpf(String cpf);

    @Query("""
            select a from AdvogadoEntity a
            join a.usuario u
            where a.disponibilidade = :disponibilidade
              and u.status = :status
            """)
    List<AdvogadoEntity> findCandidatosMatching(
            @Param("disponibilidade") DisponibilidadeAdvogadoEnum disponibilidade,
            @Param("status") StatusUsuarioEnum status
    );
}
