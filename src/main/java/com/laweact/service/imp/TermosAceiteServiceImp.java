package com.laweact.service.imp;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.laweact.config.TermosConstants;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.usuario.AceitarTermosInputDTO;
import com.laweact.dto.usuario.AceitarTermosResponseDTO;
import com.laweact.model.entity.TermosAceiteEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.repository.TermosAceiteRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.TermosAceiteService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class TermosAceiteServiceImp implements TermosAceiteService {

    private final TermosAceiteRepository termosAceiteRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public AceitarTermosResponseDTO aceitar(AceitarTermosInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        String versao = resolverVersao(input.versao());

        Optional<TermosAceiteEntity> existente = termosAceiteRepository
                .findFirstByUsuario_IdOrderByAceitoEmDesc(usuario.getId())
                .filter(t -> versao.equals(t.getVersao()));

        if (existente.isPresent()) {
            return toResponse(existente.get());
        }

        TermosAceiteEntity aceite = TermosAceiteEntity.builder()
                .usuario(usuario)
                .versao(versao)
                .scrollConfirmado(Boolean.TRUE.equals(input.scrollConfirmado()))
                .checkboxConfirmado(Boolean.TRUE.equals(input.checkboxConfirmado()))
                .build();

        TermosAceiteEntity salvo = termosAceiteRepository.save(aceite);
        log.info("Termos {} aceitos pelo usuário {}", versao, usuario.getEmail());
        return toResponse(salvo);
    }

    @Override
    public boolean possuiAceite(UUID usuarioId) {
        return termosAceiteRepository.existsByUsuario_Id(usuarioId);
    }

    @Override
    public Optional<TermosAceiteEntity> obterUltimoAceite(UUID usuarioId) {
        return termosAceiteRepository.findFirstByUsuario_IdOrderByAceitoEmDesc(usuarioId);
    }

    private UsuarioEntity obterUsuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new CustomError("Usuário não autenticado", HttpStatus.UNAUTHORIZED);
        }
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new CustomError("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private String resolverVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            return TermosConstants.VERSAO_ATUAL;
        }
        return versao.trim();
    }

    private AceitarTermosResponseDTO toResponse(TermosAceiteEntity entity) {
        return AceitarTermosResponseDTO.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .versao(entity.getVersao())
                .scrollConfirmado(entity.getScrollConfirmado())
                .checkboxConfirmado(entity.getCheckboxConfirmado())
                .aceitoEm(entity.getAceitoEm())
                .termosAceitos(true)
                .build();
    }
}
