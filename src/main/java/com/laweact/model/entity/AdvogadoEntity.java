package com.laweact.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PronomeTratamentoEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
@Table(name = "advogados")
public class AdvogadoEntity {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private UsuarioEntity usuario;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "nome_social")
    private String nomeSocial;

    @Column(name = "rg", nullable = false, length = 30)
    private String rg;

    @Column(name = "rg_orgao_emissor", nullable = false, length = 20)
    private String rgOrgaoEmissor;

    @Column(name = "rg_uf", nullable = false, length = 2)
    private String rgUf;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "nome_pai", nullable = false)
    private String nomePai;

    @Column(name = "nome_mae", nullable = false)
    private String nomeMae;

    @Enumerated(EnumType.STRING)
    @Column(name = "pronome_tratamento", nullable = false, length = 20)
    private PronomeTratamentoEnum pronomeTratamento;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "universidade", nullable = false)
    private String universidade;

    @Column(name = "curso", nullable = false)
    private String curso;

    @Column(name = "ano_formacao", nullable = false)
    private Integer anoFormacao;

    @Column(name = "atuacao_desde", nullable = false)
    private LocalDate atuacaoDesde;

    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidade", nullable = false, length = 20)
    private DisponibilidadeAdvogadoEnum disponibilidade = DisponibilidadeAdvogadoEnum.DISPONIVEL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao", nullable = false, length = 20)
    private StatusVerificacaoEnum statusVerificacao = StatusVerificacaoEnum.PENDENTE;

    @Builder.Default
    @Column(name = "media_avaliacoes", nullable = false, precision = 3, scale = 2)
    private BigDecimal mediaAvaliacoes = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_avaliacoes", nullable = false)
    private Integer totalAvaliacoes = 0;

    @CreationTimestamp
    @JsonIgnore
    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonIgnore
    @Column(name = "atualizado_em")
    private LocalDateTime updatedAt;
}
