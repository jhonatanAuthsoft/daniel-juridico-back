package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.config.AssinaturaProperties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.assinatura.AssinaturaResponseDTO;
import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.dto.assinatura.ValidarAssinaturaInputDTO;
import com.laweact.dto.job.AssinaturaReconciliacaoJobResultDTO;
import com.laweact.mapper.AssinaturaMapper;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.repository.AssinaturaRepository;
import com.laweact.repository.UsuarioRepository;
import com.laweact.service.AssinaturaService;
import com.laweact.service.AssinaturaStoreClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssinaturaServiceImp implements AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaProperties assinaturaProperties;
    private final AssinaturaMapper assinaturaMapper;
    private final List<AssinaturaStoreClient> storeClients;

    @Override
    @Transactional
    public AssinaturaEntity criarAssinaturaPendenteParaAdvogado(UsuarioEntity usuario) {
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            return null;
        }

        return assinaturaRepository.findByUsuario_Id(usuario.getId())
                .orElseGet(() -> {
                    AssinaturaEntity assinatura = AssinaturaEntity.builder()
                            .usuario(usuario)
                            .status(StatusAssinaturaEnum.PENDENTE)
                            .autoRenovacao(false)
                            .build();
                    return assinaturaRepository.save(assinatura);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public AssinaturaResponseDTO obterMinhaAssinatura() {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        return obterAssinaturaDoUsuario(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AssinaturaResponseDTO obterAssinaturaDoUsuario(UsuarioEntity usuario) {
        AssinaturaEntity assinatura = usuario.getPerfil() == PerfilUsuarioEnum.ADVOGADO
                ? assinaturaRepository.findByUsuario_Id(usuario.getId()).orElse(null)
                : null;
        return assinaturaMapper.toResponse(usuario, assinatura);
    }

    @Override
    @Transactional
    public AssinaturaResponseDTO validarCompra(ValidarAssinaturaInputDTO input) {
        UsuarioEntity usuario = obterUsuarioAutenticado();
        if (usuario.getPerfil() != PerfilUsuarioEnum.ADVOGADO) {
            throw new CustomError("Assinatura disponível apenas para advogados", HttpStatus.BAD_REQUEST);
        }

        String purchaseToken = input.purchaseToken().trim();
        String productId = input.productId().trim();

        if (!productId.equals(assinaturaProperties.getProductId())) {
            throw new CustomError("Produto de assinatura inválido", HttpStatus.BAD_REQUEST);
        }

        AssinaturaStoreClient storeClient = resolverStoreClient(input.plataforma(), purchaseToken);
        AssinaturaStoreStateDTO storeState = storeClient.consultar(input.plataforma(), purchaseToken, productId);

        // CANCELADA é aceita: quem cancelou dentro do período vigente (ex.: mês grátis) ainda
        // tem direito de restaurar a compra e usar o app até o fim dele.
        if (storeState.status() != StatusAssinaturaEnum.ATIVA
                && storeState.status() != StatusAssinaturaEnum.EM_ATRASO
                && storeState.status() != StatusAssinaturaEnum.CANCELADA) {
            throw new CustomError("Assinatura não está ativa na loja", HttpStatus.BAD_REQUEST);
        }

        assinaturaRepository.findByPurchaseToken(purchaseToken).ifPresent(existente -> {
            if (!existente.getUsuario().getId().equals(usuario.getId())) {
                throw new CustomError(
                        "Esta assinatura já está vinculada a outra conta",
                        HttpStatus.CONFLICT,
                        "SUBSCRIPTION_ALREADY_LINKED"
                );
            }
        });

        if (storeState.originalTransactionId() != null) {
            assinaturaRepository.findByOriginalTransactionId(storeState.originalTransactionId())
                    .ifPresent(existente -> {
                        if (!existente.getUsuario().getId().equals(usuario.getId())) {
                            throw new CustomError(
                                    "Esta assinatura já está vinculada a outra conta",
                                    HttpStatus.CONFLICT,
                                    "SUBSCRIPTION_ALREADY_LINKED"
                            );
                        }
                    });
        }

        AssinaturaEntity assinatura = assinaturaRepository.findByUsuario_Id(usuario.getId())
                .orElseGet(() -> AssinaturaEntity.builder().usuario(usuario).build());

        LocalDateTime agora = LocalDateTime.now();
        assinatura.setStatus(storeState.status());
        assinatura.setPlataforma(storeState.plataforma());
        assinatura.setAmbiente(storeState.ambiente());
        assinatura.setProductId(storeState.productId());
        assinatura.setPurchaseToken(storeState.purchaseToken());
        assinatura.setOriginalTransactionId(storeState.originalTransactionId());
        assinatura.setPeriodoFimEm(storeState.periodoFimEm());
        assinatura.setAutoRenovacao(storeState.autoRenovacao());
        assinatura.setUltimaSincronizacaoEm(agora);

        AssinaturaEntity salva = assinaturaRepository.save(assinatura);
        storeClient.acknowledge(input.plataforma(), purchaseToken, productId);

        return assinaturaMapper.toResponse(usuario, salva);
    }

    @Override
    @Transactional
    public AssinaturaReconciliacaoJobResultDTO reconciliar() {
        LocalDateTime agora = LocalDateTime.now();
        int assinaturasExpiradas = 0;
        int assinaturasSincronizadas = 0;

        List<AssinaturaEntity> periodoVencido = assinaturaRepository.findAssinaturasComPeriodoVencido(
                List.of(
                        StatusAssinaturaEnum.ATIVA,
                        StatusAssinaturaEnum.EM_ATRASO,
                        StatusAssinaturaEnum.CANCELADA
                ),
                agora.minus(assinaturaProperties.getGraceDuration())
        );
        for (AssinaturaEntity assinatura : periodoVencido) {
            if (assinatura.getPurchaseToken() != null && assinatura.getPlataforma() != null) {
                try {
                    AssinaturaStoreClient client = resolverStoreClient(
                            assinatura.getPlataforma(),
                            assinatura.getPurchaseToken()
                    );
                    AssinaturaStoreStateDTO state = client.consultar(
                            assinatura.getPlataforma(),
                            assinatura.getPurchaseToken(),
                            assinatura.getProductId() != null
                                    ? assinatura.getProductId()
                                    : assinaturaProperties.getProductId()
                    );
                    aplicarEstadoDaLoja(assinatura, state);
                    assinaturasSincronizadas++;
                    continue;
                } catch (Exception ignored) {
                    // fallback para expirar abaixo
                }
            }
            assinatura.setStatus(StatusAssinaturaEnum.EXPIRADA);
            assinaturaRepository.save(assinatura);
            assinaturasExpiradas++;
        }

        return AssinaturaReconciliacaoJobResultDTO.builder()
                .assinaturasExpiradas(assinaturasExpiradas)
                .assinaturasSincronizadas(assinaturasSincronizadas)
                .build();
    }

    @Override
    @Transactional
    public void bloquearAssinatura(UUID usuarioId) {
        AssinaturaEntity assinatura = obterAssinaturaObrigatoria(usuarioId);
        assinatura.setStatus(StatusAssinaturaEnum.PENDENTE);
        assinatura.setPeriodoFimEm(null);
        assinatura.setAutoRenovacao(false);
        assinaturaRepository.save(assinatura);
    }

    @Override
    @Transactional
    public void expirarAssinatura(UUID usuarioId) {
        AssinaturaEntity assinatura = obterAssinaturaObrigatoria(usuarioId);
        assinatura.setStatus(StatusAssinaturaEnum.EXPIRADA);
        assinatura.setPeriodoFimEm(LocalDateTime.now().minusMinutes(1));
        assinaturaRepository.save(assinatura);
    }

    @Override
    @Transactional
    public void renovarAssinatura(UUID usuarioId) {
        AssinaturaEntity assinatura = obterAssinaturaObrigatoria(usuarioId);
        assinatura.setStatus(StatusAssinaturaEnum.ATIVA);
        assinatura.setPeriodoFimEm(LocalDateTime.now().plus(assinaturaProperties.getFakeStore().getActiveDuration()));
        assinatura.setUltimaSincronizacaoEm(LocalDateTime.now());
        assinaturaRepository.save(assinatura);
    }

    @Override
    @Transactional
    public void resetAssinatura(UUID usuarioId) {
        AssinaturaEntity assinatura = obterAssinaturaObrigatoria(usuarioId);
        assinatura.setStatus(StatusAssinaturaEnum.PENDENTE);
        assinatura.setPlataforma(null);
        assinatura.setAmbiente(null);
        assinatura.setProductId(null);
        assinatura.setPurchaseToken(null);
        assinatura.setOriginalTransactionId(null);
        assinatura.setPeriodoFimEm(null);
        assinatura.setAutoRenovacao(false);
        assinatura.setUltimaSincronizacaoEm(null);
        assinaturaRepository.save(assinatura);
    }

    private void aplicarEstadoDaLoja(AssinaturaEntity assinatura, AssinaturaStoreStateDTO state) {
        assinatura.setStatus(state.status());
        assinatura.setPlataforma(state.plataforma());
        assinatura.setAmbiente(state.ambiente());
        assinatura.setProductId(state.productId());
        assinatura.setPurchaseToken(state.purchaseToken());
        assinatura.setOriginalTransactionId(state.originalTransactionId());
        assinatura.setPeriodoFimEm(state.periodoFimEm());
        assinatura.setAutoRenovacao(state.autoRenovacao());
        assinatura.setUltimaSincronizacaoEm(LocalDateTime.now());
        assinaturaRepository.save(assinatura);
    }

    private AssinaturaStoreClient resolverStoreClient(
            PlataformaAssinaturaEnum plataforma,
            String purchaseToken
    ) {
        return storeClients.stream()
                .filter(client -> client.supports(plataforma, purchaseToken))
                .findFirst()
                .orElseThrow(() -> new CustomError(
                        "Nenhum adaptador de loja disponível para esta plataforma",
                        HttpStatus.BAD_REQUEST
                ));
    }

    private AssinaturaEntity obterAssinaturaObrigatoria(UUID usuarioId) {
        return assinaturaRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new CustomError("Assinatura não encontrada", HttpStatus.NOT_FOUND));
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
