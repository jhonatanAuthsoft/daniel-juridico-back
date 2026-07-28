package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.JwtUtil;
import com.laweact.config.exception.CustomError;
import com.laweact.model.entity.SessaoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.SessaoRepository;
import com.laweact.service.SessaoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessaoServiceImp implements SessaoService {

    private final SessaoRepository sessaoRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public TokensSessao criar(UsuarioEntity usuario, UserDetails userDetails, String deviceId) {
        String deviceNormalizado = normalizarDeviceId(deviceId);
        if (deviceNormalizado != null) {
            sessaoRepository.deleteByUsuarioIdAndDeviceId(usuario.getId(), deviceNormalizado);
        } else {
            // Sem deviceId: uma sessão por usuário (novo login encerra as anteriores).
            sessaoRepository.deleteByUsuarioId(usuario.getId());
        }

        LocalDateTime agora = LocalDateTime.now();
        SessaoEntity sessao = SessaoEntity.builder()
                .usuario(usuario)
                .deviceId(deviceNormalizado)
                .expiraEm(agora.plusSeconds(jwtUtil.getRefreshTokenExpirationMs() / 1000))
                .build();
        SessaoEntity salva = sessaoRepository.saveAndFlush(sessao);
        return emitirTokens(salva, userDetails);
    }

    @Override
    @Transactional
    public TokensSessao renovar(SessaoEntity sessao, UserDetails userDetails) {
        LocalDateTime agora = LocalDateTime.now();
        if (!sessao.isAtiva(agora)) {
            sessaoRepository.delete(sessao);
            throw new CustomError("Sessão expirada. Faça login novamente.", HttpStatus.UNAUTHORIZED);
        }
        sessao.setExpiraEm(agora.plusSeconds(jwtUtil.getRefreshTokenExpirationMs() / 1000));
        SessaoEntity salva = sessaoRepository.save(sessao);
        return emitirTokens(salva, userDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public SessaoEntity obterAtiva(UUID sessaoId) {
        return sessaoRepository.findByIdAndExpiraEmAfter(sessaoId, LocalDateTime.now())
                .orElseThrow(() -> new CustomError("Sessão inválida. Faça login novamente.", HttpStatus.UNAUTHORIZED));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaAtiva(UUID sessaoId) {
        return sessaoRepository.existsByIdAndExpiraEmAfter(sessaoId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void encerrar(UUID sessaoId) {
        if (sessaoRepository.existsById(sessaoId)) {
            sessaoRepository.deleteById(sessaoId);
        }
    }

    @Override
    @Transactional
    public void encerrarTodasDoUsuario(UUID usuarioId) {
        sessaoRepository.deleteByUsuarioId(usuarioId);
    }

    private TokensSessao emitirTokens(SessaoEntity sessao, UserDetails userDetails) {
        String access = jwtUtil.generateAccessToken(userDetails, sessao.getId());
        String refresh = jwtUtil.generateRefreshToken(userDetails, sessao.getId());
        return new TokensSessao(access, refresh, sessao);
    }

    private String normalizarDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return deviceId.trim();
    }
}
