package com.laweact.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.laweact.model.enums.OrigemAssinaturaEventoEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assinatura_eventos")
public class AssinaturaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private AssinaturaEntity assinatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 30)
    private OrigemAssinaturaEventoEnum origem;

    @Column(name = "evento_externo_id", nullable = false, unique = true, length = 200)
    private String eventoExternoId;

    @Column(name = "tipo", nullable = false, length = 80)
    private String tipo;

    @Column(name = "payload", columnDefinition = "JSONB")
    private String payload;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime criadoEm;
}
