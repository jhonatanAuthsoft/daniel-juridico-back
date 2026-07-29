package com.laweact.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.laweact.model.enums.StatusVerificacaoEnum;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
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
@Table(name = "oabs")
public class OabEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advogado_id", nullable = false)
    private AdvogadoEntity advogado;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "data_expedicao", nullable = false)
    private LocalDate dataExpedicao;

    @Builder.Default
    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oab_fotos", joinColumns = @JoinColumn(name = "oab_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "object_key", nullable = false, length = 500)
    private List<String> fotosUrls = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 20)
    private StatusVerificacaoEnum statusValidacao = StatusVerificacaoEnum.PENDENTE;
}
