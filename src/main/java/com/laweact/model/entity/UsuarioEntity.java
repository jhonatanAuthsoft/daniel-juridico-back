package com.laweact.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
public class UsuarioEntity extends BaseEntity implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false)
    private PerfilUsuarioEnum perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuarioEnum status;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Builder.Default
    @Column(name = "tentativas_login_falhas", nullable = false)
    private Integer tentativasLoginFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Builder.Default
    @Column(name = "notificacoes_push_habilitadas", nullable = false)
    private Boolean notificacoesPushHabilitadas = true;

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return this.status == StatusUsuarioEnum.ATIVO;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return senha;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        if (status == StatusUsuarioEnum.BLOQUEADO) {
            return false;
        }
        return bloqueadoAte == null || bloqueadoAte.isBefore(LocalDateTime.now());
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
