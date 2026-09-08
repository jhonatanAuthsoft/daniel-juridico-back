package com.laweact.model.entity;

import java.time.LocalDateTime;

import com.laweact.model.enums.AmbienteAssinaturaEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "assinaturas")
public class AssinaturaEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusAssinaturaEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", length = 20)
    private PlataformaAssinaturaEnum plataforma;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", length = 20)
    private AmbienteAssinaturaEnum ambiente;

    @Column(name = "product_id", length = 120)
    private String productId;

    @Column(name = "purchase_token", length = 500, unique = true)
    private String purchaseToken;

    @Column(name = "original_transaction_id", length = 120, unique = true)
    private String originalTransactionId;

    @Column(name = "periodo_fim_em")
    private LocalDateTime periodoFimEm;

    @Builder.Default
    @Column(name = "auto_renovacao", nullable = false)
    private Boolean autoRenovacao = false;

    @Column(name = "ultima_sincronizacao_em")
    private LocalDateTime ultimaSincronizacaoEm;
}
