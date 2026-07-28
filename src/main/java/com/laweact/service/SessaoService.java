package com.laweact.service;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

import com.laweact.model.entity.SessaoEntity;
import com.laweact.model.entity.UsuarioEntity;

public interface SessaoService {

    record TokensSessao(String token, String refreshToken, SessaoEntity sessao) {}

    TokensSessao criar(UsuarioEntity usuario, UserDetails userDetails, String deviceId);

    TokensSessao renovar(SessaoEntity sessao, UserDetails userDetails);

    SessaoEntity obterAtiva(UUID sessaoId);

    boolean estaAtiva(UUID sessaoId);

    void encerrar(UUID sessaoId);

    void encerrarTodasDoUsuario(UUID usuarioId);
}
