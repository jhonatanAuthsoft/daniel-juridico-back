package com.laweact.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.laweact.model.entity.EnderecoEntity;

public interface EnderecoRepository extends JpaRepository<EnderecoEntity, UUID> {

    Optional<EnderecoEntity> findByUsuario_Id(UUID usuarioId);

    List<EnderecoEntity> findByUsuario_IdIn(Collection<UUID> usuarioIds);
}
