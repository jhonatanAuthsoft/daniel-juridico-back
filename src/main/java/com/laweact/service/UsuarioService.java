package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.usuario.AtualizarFotoInputDTO;
import com.laweact.dto.usuario.AtualizarPreferenciasInputDTO;
import com.laweact.dto.usuario.AtualizarSenhaInputDTO;
import com.laweact.dto.usuario.AtualizarSenhaResponseDTO;
import com.laweact.dto.usuario.EmailDisponivelResponseDTO;
import com.laweact.dto.usuario.FotoPerfilResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
import com.laweact.dto.usuario.PreferenciasResponseDTO;
import com.laweact.dto.usuario.RefreshTokenInputDTO;
import com.laweact.dto.usuario.RefreshTokenResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

public interface UsuarioService {
    LoginUsuarioResponseDTO login(LoginUsuarioInputDTO loginUsuarioDTO);

    RefreshTokenResponseDTO refresh(RefreshTokenInputDTO input);

    void logout(String token);

    MeResponseDTO obterUsuarioAutenticado();

    PreferenciasResponseDTO atualizarPreferenciasDoUsuarioAutenticado(AtualizarPreferenciasInputDTO input);

    FotoPerfilResponseDTO atualizarFotoDoUsuarioAutenticado(AtualizarFotoInputDTO input);

    AtualizarSenhaResponseDTO atualizarSenhaDoUsuarioAutenticado(AtualizarSenhaInputDTO input);

    EmailDisponivelResponseDTO verificarEmailDisponivel(String email);

    void excluirUsuarioAutenticado();

    UsuarioResponseDTO obterUsuarioPorId(UUID id);

    List<UsuarioResponseDTO> obterTodosUsuarios(
        int limit, int offset, String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status
    );

    long contarTodosUsuarios(String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status);
}
