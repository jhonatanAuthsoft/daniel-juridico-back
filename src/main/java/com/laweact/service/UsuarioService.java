package com.laweact.service;

import com.laweact.dto.usuario.CadastrarUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;

public interface UsuarioService {
    LoginUsuarioResponseDTO login(LoginUsuarioInputDTO loginUsuarioDTO);

    void logout(String token);

    UsuarioResponseDTO cadastrar(CadastrarUsuarioInputDTO input);

    UsuarioResponseDTO obterUsuarioAutenticado();
}
