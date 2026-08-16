package com.laweact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.DispositivoPushEntity;

public interface DispositivoPushRepository extends JpaRepository<DispositivoPushEntity, UUID> {

    Optional<DispositivoPushEntity> findByExpoPushToken(String expoPushToken);

    List<DispositivoPushEntity> findByUsuario_IdAndAtivoTrue(UUID usuarioId);
}
