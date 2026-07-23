package com.laweact.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.laweact.model.entity.UsuarioEntity;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    Optional<UsuarioEntity> findByEmail(String email);

    @Query(
        value = """
          SELECT u.*
          FROM usuarios u
          WHERE (
            CAST(:searchText AS VARCHAR) IS NULL
            OR CAST(:searchText AS VARCHAR) = ''
            OR LOWER(u.nome_completo) LIKE LOWER(CONCAT('%', CAST(:searchText AS VARCHAR), '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:searchText AS VARCHAR), '%'))
          )
          AND (
            CAST(:perfil AS VARCHAR) IS NULL
            OR u.perfil = :perfil
          )
          AND (
            CAST(:status AS VARCHAR) IS NULL
            OR u.status = :status
          )
          ORDER BY u.criado_em DESC
          LIMIT :limit
          OFFSET :offset
        """,
        nativeQuery = true
    )
    List<UsuarioEntity> findPaginated(
        @Param("searchText") String searchText,
        @Param("perfil") String perfil,
        @Param("status") String status,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    @Query(
        value = """
          SELECT COUNT(*)
          FROM usuarios u
          WHERE (
            CAST(:searchText AS VARCHAR) IS NULL
            OR CAST(:searchText AS VARCHAR) = ''
            OR LOWER(u.nome_completo) LIKE LOWER(CONCAT('%', CAST(:searchText AS VARCHAR), '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:searchText AS VARCHAR), '%'))
          )
          AND (
            CAST(:perfil AS VARCHAR) IS NULL
            OR u.perfil = :perfil
          )
          AND (
            CAST(:status AS VARCHAR) IS NULL
            OR u.status = :status
          )
        """,
        nativeQuery = true
    )
    long countWithFilter(
        @Param("searchText") String searchText,
        @Param("perfil") String perfil,
        @Param("status") String status
    );
}

