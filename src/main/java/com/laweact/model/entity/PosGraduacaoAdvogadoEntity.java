package com.laweact.model.entity;

import java.util.UUID;

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
@Table(name = "pos_graduacoes_advogado")
public class PosGraduacaoAdvogadoEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advogado_id", nullable = false)
    private AdvogadoEntity advogado;

    @Column(name = "nome_curso", nullable = false)
    private String nomeCurso;

    @Column(name = "instituicao", nullable = false)
    private String instituicao;

    @Column(name = "ano_formacao")
    private Integer anoFormacao;

    public UUID getAdvogadoId() {
        return advogado != null ? advogado.getUsuarioId() : null;
    }
}
