package com.laweact.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laweact.model.enums.ReferenciaNotificacaoEnum;
import com.laweact.model.enums.StatusEnvioNotificacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;

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
@Table(name = "notificacoes")
public class NotificacaoEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private UsuarioEntity destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remetente_id", nullable = false)
    private UsuarioEntity remetente;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoNotificacaoEnum tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_tipo", nullable = false, length = 50)
    private ReferenciaNotificacaoEnum referenciaTipo;

    @Column(name = "referencia_id", nullable = false)
    private UUID referenciaId;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status_envio", nullable = false, length = 20)
    private StatusEnvioNotificacaoEnum statusEnvio = StatusEnvioNotificacaoEnum.PENDENTE;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "erro_envio", length = 500)
    private String erroEnvio;
}
