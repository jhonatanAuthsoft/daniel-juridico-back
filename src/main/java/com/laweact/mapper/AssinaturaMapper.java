package com.laweact.mapper;

import org.springframework.stereotype.Component;

import com.laweact.config.AssinaturaProperties;
import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.service.AssinaturaAcessoService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AssinaturaMapper {

    private final AssinaturaAcessoService assinaturaAcessoService;
    private final AssinaturaProperties assinaturaProperties;

    public AssinaturaResponseDTO toResponse(UsuarioEntity usuario, AssinaturaEntity assinatura) {
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            return assinaturaLiberadaParaNaoAdvogado();
        }

        if (assinatura == null) {
            return assinaturaBloqueada();
        }

        return AssinaturaResponseDTO.builder()
                .status(assinatura.getStatus())
                .acessoLiberado(assinaturaAcessoService.isAcessoLiberado(usuario, assinatura))
                .periodoFimEm(assinatura.getPeriodoFimEm())
                .plataforma(assinatura.getPlataforma())
                .ambiente(assinatura.getAmbiente())
                .productId(assinatura.getProductId() != null
                        ? assinatura.getProductId()
                        : assinaturaProperties.getProductId())
                .autoRenovacao(Boolean.TRUE.equals(assinatura.getAutoRenovacao()))
                .build();
    }

    private AssinaturaResponseDTO assinaturaLiberadaParaNaoAdvogado() {
        return AssinaturaResponseDTO.builder()
                .status(StatusAssinaturaEnum.ATIVA)
                .acessoLiberado(true)
                .productId(assinaturaProperties.getProductId())
                .autoRenovacao(false)
                .build();
    }

    private AssinaturaResponseDTO assinaturaBloqueada() {
        return AssinaturaResponseDTO.builder()
                .status(StatusAssinaturaEnum.PENDENTE)
                .acessoLiberado(false)
                .productId(assinaturaProperties.getProductId())
                .autoRenovacao(false)
                .build();
    }
}
