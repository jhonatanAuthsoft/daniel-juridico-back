package com.laweact.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tokens_revogados")
public class TokenRevogadoEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "revogado_em", nullable = false)
    @Builder.Default
    private LocalDateTime revogadoEm = LocalDateTime.now();
}
