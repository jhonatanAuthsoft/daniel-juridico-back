package com.laweact.model.entity;

import com.laweact.model.enums.NivelLocalidadeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "solicitacao_matches")
public class SolicitacaoMatchEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoEntity solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advogado_id", nullable = false)
    private AdvogadoEntity advogado;

    @Column(name = "posicao", nullable = false)
    private Integer posicao;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_localidade", nullable = false, length = 20)
    private NivelLocalidadeEnum nivelLocalidade;

    @Column(name = "pontos_modalidade", nullable = false)
    private Integer pontosModalidade;

    @Column(name = "pontos_localidade", nullable = false)
    private Integer pontosLocalidade;

    @Column(name = "pontos_especialidade", nullable = false)
    private Integer pontosEspecialidade;

    @Column(name = "pontos_subespecialidade", nullable = false)
    private Integer pontosSubespecialidade;

    @Column(name = "pontos_experiencia", nullable = false)
    private Integer pontosExperiencia;

    @Column(name = "pontos_cobranca", nullable = false)
    private Integer pontosCobranca;
}
