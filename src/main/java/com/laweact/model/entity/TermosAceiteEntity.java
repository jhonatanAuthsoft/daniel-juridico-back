package com.laweact.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "termos_aceite")
public class TermosAceiteEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private UsuarioEntity usuario;

    @Column(name = "versao", nullable = false, length = 50)
    private String versao;

    @Builder.Default
    @Column(name = "scroll_confirmado", nullable = false)
    private Boolean scrollConfirmado = false;

    @Builder.Default
    @Column(name = "checkbox_confirmado", nullable = false)
    private Boolean checkboxConfirmado = true;

    @CreationTimestamp
    @Column(name = "aceito_em", nullable = false, updatable = false)
    private LocalDateTime aceitoEm;

    public UUID getUsuarioId() {
        return usuario != null ? usuario.getId() : null;
    }
}
