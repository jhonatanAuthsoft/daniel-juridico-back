package com.laweact.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.NotificacaoEntity;
import com.laweact.model.enums.TipoNotificacaoEnum;

public interface NotificacaoRepository extends JpaRepository<NotificacaoEntity, UUID> {

    List<NotificacaoEntity> findByDestinatario_IdOrderByCreatedAtDesc(
            UUID destinatarioId,
            Pageable pageable
    );

    boolean existsByDestinatario_IdAndLidaEmIsNull(UUID destinatarioId);

    Optional<NotificacaoEntity> findByIdAndDestinatario_Id(UUID id, UUID destinatarioId);

    Optional<NotificacaoEntity> findFirstByReferenciaIdAndTipoOrderByCreatedAtAsc(
            UUID referenciaId,
            TipoNotificacaoEnum tipo
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificacaoEntity n
            SET n.lidaEm = :agora
            WHERE n.destinatario.id = :destinatarioId
              AND n.lidaEm IS NULL
            """)
    int marcarTodasLidas(
            @Param("destinatarioId") UUID destinatarioId,
            @Param("agora") LocalDateTime agora
    );
}
