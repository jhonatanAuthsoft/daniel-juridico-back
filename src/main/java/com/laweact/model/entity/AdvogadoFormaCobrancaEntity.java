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
@Table(name = "advogado_formas_cobranca")
public class AdvogadoFormaCobrancaEntity {

    @EmbeddedId
    private AdvogadoFormaCobrancaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("advogadoId")
    @JoinColumn(name = "advogado_id")
    private AdvogadoEntity advogado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("formaCobrancaId")
    @JoinColumn(name = "forma_cobranca_id")
    private FormaCobrancaEntity formaCobranca;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvogadoFormaCobrancaId implements Serializable {
        @Column(name = "advogado_id")
        private UUID advogadoId;

        @Column(name = "forma_cobranca_id")
        private UUID formaCobrancaId;
    }
}
