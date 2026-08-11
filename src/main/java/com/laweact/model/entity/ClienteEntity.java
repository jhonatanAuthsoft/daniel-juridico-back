package com.laweact.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laweact.model.enums.PronomesEnum;
import com.laweact.model.enums.TipoDocumentoEnum;

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
@Table(name = "clientes")
public class ClienteEntity {

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

    @Column(name = "profissao")
    private String profissao;

    @Column(name = "razao_social")
    private String razaoSocial;

    @Column(name = "area_atuacao")
    private String areaAtuacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumentoEnum tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "rg", length = 30)
    private String rg;

    @Column(name = "rg_orgao_emissor", length = 20)
    private String rgOrgaoEmissor;

    @Column(name = "rg_uf", length = 2)
    private String rgUf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "pronomes", nullable = false, length = 20)
    private PronomesEnum pronomes;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "faixa_renda", length = 100)
    private String faixaRenda;

    @Column(name = "estado_civil", length = 50)
    private String estadoCivil;

    @CreationTimestamp
    @JsonIgnore
    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonIgnore
    @Column(name = "atualizado_em")
    private LocalDateTime updatedAt;
}
