package com.laweact.service.imp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.apple.itunes.storekit.client.APIException;
import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.Status;
import com.apple.itunes.storekit.model.LastTransactionsItem;
import com.apple.itunes.storekit.model.StatusResponse;
import com.apple.itunes.storekit.model.SubscriptionGroupIdentifierItem;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.model.enums.AmbienteAssinaturaEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.service.AssinaturaStoreClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(prefix = "laweact.assinatura.apple", name = "enabled", havingValue = "true")
public class AppleStoreClientImp implements AssinaturaStoreClient {

    private final AppStoreServerAPIClient appStoreServerAPIClient;
    private final SignedDataVerifier signedDataVerifier;

    @Override
    public boolean supports(PlataformaAssinaturaEnum plataforma, String purchaseToken) {
        return plataforma == PlataformaAssinaturaEnum.IOS
                && purchaseToken != null
                && !purchaseToken.startsWith("fake:");
    }

    @Override
    public AssinaturaStoreStateDTO consultar(
            PlataformaAssinaturaEnum plataforma,
            String purchaseToken,
            String productId
    ) {
        try {
            if (purchaseToken.contains(".")) {
                return fromSignedTransaction(purchaseToken, productId);
            }

            StatusResponse statuses = appStoreServerAPIClient.getAllSubscriptionStatuses(
                    purchaseToken,
                    Status.values()
            );
            return fromSubscriptionStatuses(statuses, productId, purchaseToken);
        } catch (APIException | VerificationException | java.io.IOException e) {
            log.error("Erro ao consultar assinatura Apple: {}", e.getMessage());
            throw new CustomError("Falha ao validar assinatura na App Store", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void acknowledge(PlataformaAssinaturaEnum plataforma, String purchaseToken, String productId) {
        // Apple não exige acknowledge separado
    }

    private AssinaturaStoreStateDTO fromSignedTransaction(String signedTransaction, String productId)
            throws VerificationException {
        JWSTransactionDecodedPayload payload = signedDataVerifier.verifyAndDecodeTransaction(signedTransaction);
        if (!productId.equals(payload.getProductId())) {
            throw new CustomError("Produto da transação não confere", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime periodoFim = toLocalDateTime(payload.getExpiresDate());
        return AssinaturaStoreStateDTO.builder()
                .status(isAtivo(periodoFim) ? StatusAssinaturaEnum.ATIVA : StatusAssinaturaEnum.EXPIRADA)
                .plataforma(PlataformaAssinaturaEnum.IOS)
                .ambiente(mapAmbiente(payload.getEnvironment()))
                .productId(payload.getProductId())
                .purchaseToken(signedTransaction)
                .originalTransactionId(payload.getOriginalTransactionId())
                .periodoFimEm(periodoFim)
                .autoRenovacao(true)
                .build();
    }

    private AssinaturaStoreStateDTO fromSubscriptionStatuses(
            StatusResponse statuses,
            String productId,
            String originalTransactionId
    ) throws VerificationException {
        List<SubscriptionGroupIdentifierItem> groups = statuses.getData() != null
                ? statuses.getData()
                : Collections.emptyList();

        for (SubscriptionGroupIdentifierItem group : groups) {
            List<LastTransactionsItem> transactions = group.getLastTransactions() != null
                    ? group.getLastTransactions()
                    : Collections.emptyList();
            for (LastTransactionsItem subscriptionStatus : transactions) {
                JWSTransactionDecodedPayload payload = signedDataVerifier.verifyAndDecodeTransaction(
                        subscriptionStatus.getSignedTransactionInfo()
                );
                if (!productId.equals(payload.getProductId())) {
                    continue;
                }

                LocalDateTime periodoFim = toLocalDateTime(payload.getExpiresDate());
                StatusAssinaturaEnum status = mapAppleStatus(subscriptionStatus.getStatus(), periodoFim);
                return AssinaturaStoreStateDTO.builder()
                        .status(status)
                        .plataforma(PlataformaAssinaturaEnum.IOS)
                        .ambiente(mapAmbiente(payload.getEnvironment()))
                        .productId(payload.getProductId())
                        .purchaseToken(originalTransactionId)
                        .originalTransactionId(payload.getOriginalTransactionId())
                        .periodoFimEm(periodoFim)
                        .autoRenovacao(subscriptionStatus.getStatus() != Status.EXPIRED)
                        .build();
            }
        }
        throw new CustomError("Assinatura não encontrada na App Store", HttpStatus.NOT_FOUND);
    }

    private StatusAssinaturaEnum mapAppleStatus(Status status, LocalDateTime periodoFim) {
        if (status == Status.ACTIVE || status == Status.BILLING_GRACE_PERIOD) {
            return StatusAssinaturaEnum.ATIVA;
        }
        if (status == Status.BILLING_RETRY) {
            return StatusAssinaturaEnum.EM_ATRASO;
        }
        if (isAtivo(periodoFim)) {
            return StatusAssinaturaEnum.ATIVA;
        }
        return StatusAssinaturaEnum.EXPIRADA;
    }

    private boolean isAtivo(LocalDateTime periodoFim) {
        return periodoFim == null || periodoFim.isAfter(LocalDateTime.now());
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }

    private AmbienteAssinaturaEnum mapAmbiente(Environment environment) {
        return environment == Environment.PRODUCTION
                ? AmbienteAssinaturaEnum.PRODUCAO
                : AmbienteAssinaturaEnum.SANDBOX;
    }
}
