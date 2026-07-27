package com.laweact.model.entity;

import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auditoria_eventos")
public class AuditoriaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "evento", nullable = false, length = 100)
    private String evento;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
