package com.laweact.model.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name = "advogado_modalidades")
public class AdvogadoModalidadeEntity {

    @EmbeddedId
    private AdvogadoModalidadeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("advogadoId")
    @JoinColumn(name = "advogado_id")
    private AdvogadoEntity advogado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("modalidadeId")
    @JoinColumn(name = "modalidade_id")
    private ModalidadeAtuacaoEntity modalidade;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvogadoModalidadeId implements Serializable {
        @Column(name = "advogado_id")
        private UUID advogadoId;

        @Column(name = "modalidade_id")
        private UUID modalidadeId;
    }
}
