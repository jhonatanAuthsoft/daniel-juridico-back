package com.laweact.service.imp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apple.itunes.storekit.model.AutoRenewStatus;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laweact.dto.assinatura.AppleNotificationInputDTO;
import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.dto.assinatura.GooglePubSubNotificationInputDTO;
import com.laweact.model.entity.AssinaturaEntity;
import com.laweact.model.entity.AssinaturaEventoEntity;
import com.laweact.model.enums.OrigemAssinaturaEventoEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.repository.AssinaturaEventoRepository;
import com.laweact.repository.AssinaturaRepository;
import com.laweact.service.AssinaturaNotificationService;
import com.laweact.service.AssinaturaStoreClient;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AssinaturaNotificationServiceImp implements AssinaturaNotificationService {

    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaEventoRepository assinaturaEventoRepository;
    private final Optional<SignedDataVerifier> signedDataVerifier;
    private final List<AssinaturaStoreClient> storeClients;
    private final ObjectMapper objectMapper;

    public AssinaturaNotificationServiceImp(
            AssinaturaRepository assinaturaRepository,
            AssinaturaEventoRepository assinaturaEventoRepository,
            @Autowired(required = false) SignedDataVerifier signedDataVerifier,
            List<AssinaturaStoreClient> storeClients,
            ObjectMapper objectMapper
    ) {
        this.assinaturaRepository = assinaturaRepository;
        this.assinaturaEventoRepository = assinaturaEventoRepository;
        this.signedDataVerifier = Optional.ofNullable(signedDataVerifier);
        this.storeClients = storeClients;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processarApple(AppleNotificationInputDTO input) {
        if (signedDataVerifier.isEmpty()) {
            log.warn("Webhook Apple recebido mas integração Apple não está habilitada");
            return;
        }

        try {
            ResponseBodyV2DecodedPayload payload = signedDataVerifier.get()
                    .verifyAndDecodeNotification(input.signedPayload());

            String eventoId = payload.getNotificationUUID();
            if (assinaturaEventoRepository.existsByEventoExternoId(eventoId)) {
                return;
            }

            if (payload.getData() == null || payload.getData().getSignedTransactionInfo() == null) {
                return;
            }

            JWSTransactionDecodedPayload transaction = signedDataVerifier.get()
                    .verifyAndDecodeTransaction(payload.getData().getSignedTransactionInfo());

            AssinaturaEntity assinatura = assinaturaRepository
                    .findByOriginalTransactionId(transaction.getOriginalTransactionId())
                    .or(() -> assinaturaRepository.findByPurchaseToken(transaction.getOriginalTransactionId()))
                    .orElse(null);

            if (assinatura == null) {
                log.info("Webhook Apple sem assinatura local para originalTransactionId={}",
                        transaction.getOriginalTransactionId());
                return;
            }

            registrarEvento(assinatura, OrigemAssinaturaEventoEnum.APPLE, eventoId,
                    payload.getNotificationType() != null ? payload.getNotificationType().name() : "UNKNOWN",
                    input.signedPayload());

            AutoRenewStatus autoRenewStatus = null;
            if (payload.getData().getSignedRenewalInfo() != null) {
                autoRenewStatus = signedDataVerifier.get()
                        .verifyAndDecodeRenewalInfo(payload.getData().getSignedRenewalInfo())
                        .getAutoRenewStatus();
            }

            atualizarPorTipoApple(
                    assinatura,
                    payload.getNotificationType() != null ? payload.getNotificationType().name() : null,
                    transaction,
                    autoRenewStatus
            );
        } catch (VerificationException e) {
            log.error("Falha ao verificar notificação Apple: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processarGoogle(GooglePubSubNotificationInputDTO input) {
        String eventoId = input.message().messageId();
        if (assinaturaEventoRepository.existsByEventoExternoId(eventoId)) {
            return;
        }

        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(input.message().data()));
            var node = objectMapper.readTree(decoded);
            String purchaseToken = node.path("subscriptionNotification").path("purchaseToken").asText(null);
            if (purchaseToken == null || purchaseToken.isBlank()) {
                return;
            }

            AssinaturaEntity assinatura = assinaturaRepository.findByPurchaseToken(purchaseToken).orElse(null);
            if (assinatura == null) {
                log.info("Webhook Google sem assinatura local para purchaseToken={}", purchaseToken);
                return;
            }

            registrarEvento(assinatura, OrigemAssinaturaEventoEnum.GOOGLE, eventoId,
                    node.path("subscriptionNotification").path("notificationType").asText("UNKNOWN"),
                    decoded);

            if (storeClients.stream().anyMatch(c -> c.supports(PlataformaAssinaturaEnum.ANDROID, purchaseToken))) {
                AssinaturaStoreClient client = storeClients.stream()
                        .filter(c -> c.supports(PlataformaAssinaturaEnum.ANDROID, purchaseToken))
                        .findFirst()
                        .orElseThrow();
                AssinaturaStoreStateDTO state = client.consultar(
                        PlataformaAssinaturaEnum.ANDROID,
                        purchaseToken,
                        assinatura.getProductId()
                );
                aplicarEstado(assinatura, state);
            }
        } catch (Exception e) {
            log.error("Falha ao processar notificação Google: {}", e.getMessage());
        }
    }

    void atualizarPorTipoApple(
            AssinaturaEntity assinatura,
            String notificationType,
            JWSTransactionDecodedPayload transaction,
            AutoRenewStatus autoRenewStatus
    ) {
        if (autoRenewStatus != null) {
            assinatura.setAutoRenovacao(autoRenewStatus == AutoRenewStatus.ON);
        }

        if ("EXPIRED".equals(notificationType)
                || "REFUND".equals(notificationType)
                || "GRACE_PERIOD_EXPIRED".equals(notificationType)) {
            assinatura.setStatus(StatusAssinaturaEnum.EXPIRADA);
        } else if ("DID_FAIL_TO_RENEW".equals(notificationType)) {
            assinatura.setStatus(StatusAssinaturaEnum.EM_ATRASO);
        } else if ("DID_RENEW".equals(notificationType) || "SUBSCRIBED".equals(notificationType)) {
            assinatura.setStatus(StatusAssinaturaEnum.ATIVA);
        } else if ("DID_CHANGE_RENEWAL_STATUS".equals(notificationType) && autoRenewStatus != null) {
            // Desligou a renovação (tipicamente cancelando dentro do mês grátis): registramos o
            // cancelamento, mas o acesso segue até periodoFimEm — quem cancela não é cobrado e
            // também não perde o app no mesmo instante.
            assinatura.setStatus(autoRenewStatus == AutoRenewStatus.ON
                    ? StatusAssinaturaEnum.ATIVA
                    : StatusAssinaturaEnum.CANCELADA);
        }

        if (transaction.getExpiresDate() != null) {
            assinatura.setPeriodoFimEm(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(transaction.getExpiresDate()),
                    java.time.ZoneId.systemDefault()
            ));
        }
        assinatura.setUltimaSincronizacaoEm(LocalDateTime.now());
        assinaturaRepository.save(assinatura);
    }

    private void aplicarEstado(AssinaturaEntity assinatura, AssinaturaStoreStateDTO state) {
        assinatura.setStatus(state.status());
        assinatura.setPeriodoFimEm(state.periodoFimEm());
        assinatura.setAutoRenovacao(state.autoRenovacao());
        assinatura.setUltimaSincronizacaoEm(LocalDateTime.now());
        assinaturaRepository.save(assinatura);
    }

    private void registrarEvento(
            AssinaturaEntity assinatura,
            OrigemAssinaturaEventoEnum origem,
            String eventoExternoId,
            String tipo,
            String payload
    ) {
        assinaturaEventoRepository.save(AssinaturaEventoEntity.builder()
                .assinatura(assinatura)
                .origem(origem)
                .eventoExternoId(eventoExternoId)
                .tipo(tipo)
                .payload(payload)
                .build());
    }
}
