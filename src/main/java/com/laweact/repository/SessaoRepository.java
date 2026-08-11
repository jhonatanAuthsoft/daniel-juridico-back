package com.laweact.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.model.entity.SessaoEntity;

public interface SessaoRepository extends JpaRepository<SessaoEntity, UUID> {

    Optional<SessaoEntity> findByIdAndExpiraEmAfter(UUID id, LocalDateTime agora);

    boolean existsByIdAndExpiraEmAfter(UUID id, LocalDateTime agora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM SessaoEntity s WHERE s.usuario.id = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM SessaoEntity s WHERE s.usuario.id = :usuarioId AND s.deviceId = :deviceId")
    int deleteByUsuarioIdAndDeviceId(@Param("usuarioId") UUID usuarioId, @Param("deviceId") String deviceId);
}
