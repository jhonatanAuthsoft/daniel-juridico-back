package com.laweact.mapper;

import org.springframework.stereotype.Component;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
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

    public LoginUsuarioResponseDTO toLoginResponse(
            UsuarioEntity usuario,
            ClienteDetalheResponseDTO cliente,
            AdvogadoDetalheResponseDTO advogado,
            String token
    ) {
        return LoginUsuarioResponseDTO.builder()
                .usuario(toResponseDTO(usuario))
                .cliente(cliente)
                .advogado(advogado)
                .token(token)
                .build();
    }

    public MeResponseDTO toMeResponse(
            UsuarioEntity usuario,
            ClienteDetalheResponseDTO cliente,
            AdvogadoDetalheResponseDTO advogado
    ) {
        return MeResponseDTO.builder()
                .usuario(toResponseDTO(usuario))
                .cliente(cliente)
                .advogado(advogado)
                .build();
    }
}
