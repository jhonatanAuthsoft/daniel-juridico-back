package com.laweact.service.imp;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.LogAcessoTelaResponseDTO;
import com.laweact.dto.usuario.RegistrarAcessoTelaInputDTO;
import com.laweact.model.entity.LogAcessoTelaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.LogAcessoTelaRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.LogAcessoTelaService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogAcessoTelaServiceImp implements LogAcessoTelaService {

    private final LogAcessoTelaRepository logAcessoTelaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public LogAcessoTelaResponseDTO registrar(RegistrarAcessoTelaInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        LogAcessoTelaEntity salvo = logAcessoTelaRepository.saveAndFlush(LogAcessoTelaEntity.builder()
                .usuario(usuario)
                .tela(input.tela())
                .build());

        return LogAcessoTelaResponseDTO.builder()
                .id(salvo.getId())
                .usuarioId(usuario.getId())
                .tela(salvo.getTela())
                .acessadoEm(salvo.getCreatedAt())
                .build();
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
