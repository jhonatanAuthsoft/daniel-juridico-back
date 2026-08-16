package com.laweact.service.imp;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.dispositivo.DispositivoPushResponseDTO;
import com.laweact.dto.dispositivo.RegistrarDispositivoPushInputDTO;
import com.laweact.dto.dispositivo.RemoverDispositivoPushInputDTO;
import com.laweact.model.entity.DispositivoPushEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.DispositivoPushRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.DispositivoPushService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DispositivoPushServiceImp implements DispositivoPushService {

    private final DispositivoPushRepository dispositivoPushRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public DispositivoPushResponseDTO registrar(RegistrarDispositivoPushInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        String token = input.expoPushToken().trim();
        LocalDateTime agora = LocalDateTime.now();

        DispositivoPushEntity dispositivo = dispositivoPushRepository.findByExpoPushToken(token)
                .map(existente -> {
                    existente.setUsuario(usuario);
                    existente.setPlataforma(input.plataforma());
                    existente.setAtivo(true);
                    existente.setUltimoRegistroEm(agora);
                    return existente;
                })
                .orElseGet(() -> DispositivoPushEntity.builder()
                        .usuario(usuario)
                        .expoPushToken(token)
                        .plataforma(input.plataforma())
                        .ativo(true)
                        .ultimoRegistroEm(agora)
                        .build());

        return toResponse(dispositivoPushRepository.save(dispositivo));
    }

    @Override
    @Transactional
    public DispositivoPushResponseDTO remover(RemoverDispositivoPushInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        String token = input.expoPushToken().trim();

        DispositivoPushEntity dispositivo = dispositivoPushRepository.findByExpoPushToken(token)
                .orElseThrow(() -> new CustomError("Dispositivo não encontrado", HttpStatus.NOT_FOUND));

        if (!dispositivo.getUsuario().getId().equals(usuario.getId())) {
            throw new CustomError("Dispositivo de outro usuário", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        dispositivo.setAtivo(false);
        return toResponse(dispositivoPushRepository.save(dispositivo));
    }

    private DispositivoPushResponseDTO toResponse(DispositivoPushEntity entity) {
        return DispositivoPushResponseDTO.builder()
                .id(entity.getId())
                .expoPushToken(entity.getExpoPushToken())
                .plataforma(entity.getPlataforma())
                .ativo(entity.getAtivo())
                .ultimoRegistroEm(entity.getUltimoRegistroEm())
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
