package com.laweact.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.enums.StatusAssinaturaEnum;

public interface AssinaturaRepository extends JpaRepository<AssinaturaEntity, UUID> {

    Optional<AssinaturaEntity> findByUsuario_Id(UUID usuarioId);

    Optional<AssinaturaEntity> findByPurchaseToken(String purchaseToken);

    Optional<AssinaturaEntity> findByOriginalTransactionId(String originalTransactionId);

    @Query("""
            SELECT a FROM AssinaturaEntity a
            WHERE a.status IN :statuses
              AND a.periodoFimEm IS NOT NULL
              AND a.periodoFimEm < :agora
            """)
    List<AssinaturaEntity> findAssinaturasComPeriodoVencido(
            @Param("statuses") List<StatusAssinaturaEnum> statuses,
            @Param("agora") LocalDateTime agora
    );
}
