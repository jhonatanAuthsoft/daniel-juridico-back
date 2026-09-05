package com.laweact.mapper;

import org.springframework.stereotype.Component;

import com.laweact.dto.advogado.AdvogadoDetalheResponseDTO;
import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.dto.cliente.ClienteDetalheResponseDTO;
import com.laweact.dto.usuario.LoginUsuarioResponseDTO;
import com.laweact.dto.usuario.MeResponseDTO;
import com.laweact.dto.usuario.UsuarioResponseDTO;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.service.TermosAceiteService;

@Component
public class UsuarioMapper {

    private final TermosAceiteService termosAceiteService;

    public UsuarioMapper(TermosAceiteService termosAceiteService) {
        this.termosAceiteService = termosAceiteService;
    }

    public UsuarioResponseDTO toResponseDTO(UsuarioEntity usuario) {
        var ultimoAceite = termosAceiteService.obterUltimoAceite(usuario.getId());
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .status(usuario.getStatus())
                .nomeCompleto(usuario.getNomeCompleto())
                .perfil(usuario.getPerfil())
                .telefone(usuario.getTelefone())
                .termosAceitos(ultimoAceite.isPresent())
                .notificacoesPushHabilitadas(Boolean.TRUE.equals(usuario.getNotificacoesPushHabilitadas()))
                .build();
    }

    public LoginUsuarioResponseDTO toLoginResponse(
            UsuarioEntity usuario,
            ClienteDetalheResponseDTO cliente,
            AdvogadoDetalheResponseDTO advogado,
            String token,
            String refreshToken
    ) {
        return LoginUsuarioResponseDTO.builder()
                .usuario(toResponseDTO(usuario))
                .cliente(cliente)
                .advogado(advogado)
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public MeResponseDTO toMeResponse(
            UsuarioEntity usuario,
            ClienteDetalheResponseDTO cliente,
            AdvogadoDetalheResponseDTO advogado,
            AssinaturaResponseDTO assinatura
    ) {
        return MeResponseDTO.builder()
                .usuario(toResponseDTO(usuario))
                .cliente(cliente)
                .advogado(advogado)
                .assinatura(assinatura)
                .build();
    }
}
