package com.laweact.model.entity;

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
@Table(name = "advogado_especialidades")
public class AdvogadoEspecialidadeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advogado_id", nullable = false)
    private AdvogadoEntity advogado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidade_id")
    private EspecialidadeEntity especialidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subespecialidade_id")
    private SubespecialidadeEntity subespecialidade;

    @Column(name = "especialidade_livre")
    private String especialidadeLivre;

    @Column(name = "subespecialidade_livre")
    private String subespecialidadeLivre;
}
