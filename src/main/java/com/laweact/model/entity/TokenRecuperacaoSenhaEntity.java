package com.laweact.model.entity;

import java.time.LocalDateTime;

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
@Table(name = "tokens_recuperacao_senha")
public class TokenRecuperacaoSenhaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "invalidado_em")
    private LocalDateTime invalidadoEm;

    public boolean isAtivo(LocalDateTime agora) {
        return usadoEm == null
                && invalidadoEm == null
                && expiraEm != null
                && expiraEm.isAfter(agora);
    }
}
