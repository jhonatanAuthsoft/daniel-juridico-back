package com.laweact.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.laweact.config.AssinaturaProperties;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssinaturaAcessoService {

    private final AssinaturaProperties assinaturaProperties;

    public boolean isAcessoLiberado(UsuarioEntity usuario, AssinaturaEntity assinatura) {
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            return true;
        }
        if (assinatura == null) {
            return false;
        }

        LocalDateTime agora = LocalDateTime.now();
        StatusAssinaturaEnum status = assinatura.getStatus();

        if (status == StatusAssinaturaEnum.ATIVA) {
            return assinatura.getPeriodoFimEm() == null || agora.isBefore(assinatura.getPeriodoFimEm());
        }
        // Cancelou a renovação, mas o período já concedido continua valendo: quem desiste
        // durante o mês grátis usa o app até o fim dele e não é cobrado.
        if (status == StatusAssinaturaEnum.CANCELADA) {
            return assinatura.getPeriodoFimEm() != null && agora.isBefore(assinatura.getPeriodoFimEm());
        }
        if (status == StatusAssinaturaEnum.EM_ATRASO) {
            if (assinatura.getPeriodoFimEm() == null) {
                return false;
            }
            return agora.isBefore(assinatura.getPeriodoFimEm().plus(assinaturaProperties.getGraceDuration()));
        }
        return false;
    }
}
