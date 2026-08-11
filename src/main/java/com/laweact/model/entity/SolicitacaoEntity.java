package com.laweact.model.entity;

import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.StatusSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

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
@Table(name = "solicitacoes")
public class SolicitacaoEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 30)
    private ModalidadeSolicitacaoEnum modalidade;

    @Column(name = "especialidade_codigo", nullable = false, length = 50)
    private String especialidadeCodigo;

    @Column(name = "subespecialidade_codigo", length = 50)
    private String subespecialidadeCodigo;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "cidade", nullable = false, length = 120)
    private String cidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgencia", nullable = false, length = 30)
    private UrgenciaSolicitacaoEnum urgencia;

    @Column(name = "descricao", nullable = false, length = 800)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_cobranca", length = 30)
    private FormaCobrancaSolicitacaoEnum formaCobranca;

    @Column(name = "experiencia_minima_meses")
    private Integer experienciaMinimaMeses;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusSolicitacaoEnum status = StatusSolicitacaoEnum.ABERTA;
}
