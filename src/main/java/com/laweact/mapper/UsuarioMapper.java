package com.laweact.mapper;

import org.springframework.stereotype.Component;

import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.entity.UsuarioEntity;

@Component
public class UsuarioMapper {

    public UsuarioResponseDTO toResponseDTO(UsuarioEntity usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .status(usuario.getStatus())
                .nomeCompleto(usuario.getNomeCompleto())
                .perfil(usuario.getPerfil())
                .telefone(usuario.getTelefone())
                .build();
    }

    public LoginUsuarioResponseDTO loginUserResponseToDTO(UsuarioEntity usuario, String token) {
        return LoginUsuarioResponseDTO.builder()
                .usuario(toResponseDTO(usuario))
                .token(token)
                .build();
    }
}
