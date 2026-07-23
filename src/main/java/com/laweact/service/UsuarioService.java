package com.laweact.service;

import java.util.List;
import java.util.UUID;

import com.laweact.dto.usuario.CadastrarUsuarioInputDTO;
import com.laweact.dto.usuario.EditarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

public interface UsuarioService {
    LoginUsuarioResponseDTO login(LoginUsuarioInputDTO loginUsuarioDTO);

    void logout(String token);

    UsuarioResponseDTO cadastrar(CadastrarUsuarioInputDTO input);

    UsuarioResponseDTO obterUsuarioAutenticado();

    void redefinirSenha(RedefinirSenhaInputDTO redefinirSenhaInputDTO);

    UsuarioResponseDTO editar(UUID id, EditarUsuarioInputDTO input);

    void excluir(UUID id);

    UsuarioResponseDTO obterUsuarioPorId(UUID id);

    List<UsuarioResponseDTO> obterTodosUsuarios(
        int limit, int offset, String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status
    );

    long contarTodosUsuarios(String searchText, PerfilUsuarioEnum perfil, StatusUsuarioEnum status);
}

