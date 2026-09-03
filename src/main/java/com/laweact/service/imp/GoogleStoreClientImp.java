package com.laweact.service.imp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.SubscriptionPurchaseLineItem;
import com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2;
import com.laweact.config.AssinaturaProperties;
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
@ConditionalOnProperty(prefix = "laweact.assinatura.google", name = "enabled", havingValue = "true")
public class GoogleStoreClientImp implements AssinaturaStoreClient {

    private static final String STATE_ACTIVE = "SUBSCRIPTION_STATE_ACTIVE";
    private static final String STATE_ON_HOLD = "SUBSCRIPTION_STATE_ON_HOLD";
    private static final String STATE_IN_GRACE = "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
    private static final String STATE_CANCELED = "SUBSCRIPTION_STATE_CANCELED";
    private static final String STATE_EXPIRED = "SUBSCRIPTION_STATE_EXPIRED";

    private final AndroidPublisher androidPublisher;
    private final AssinaturaProperties assinaturaProperties;

    @Override
    public boolean supports(PlataformaAssinaturaEnum plataforma, String purchaseToken) {
        return plataforma == PlataformaAssinaturaEnum.ANDROID
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
            SubscriptionPurchaseV2 purchase = androidPublisher.purchases().subscriptionsv2()
                    .get(assinaturaProperties.getGoogle().getPackageName(), purchaseToken)
                    .execute();

            LocalDateTime periodoFim = extractExpiry(purchase);
            StatusAssinaturaEnum status = mapGoogleStatus(purchase.getSubscriptionState(), periodoFim);

            return AssinaturaStoreStateDTO.builder()
                    .status(status)
                    .plataforma(PlataformaAssinaturaEnum.ANDROID)
                    .ambiente(AmbienteAssinaturaEnum.PRODUCAO)
                    .productId(productId)
                    .purchaseToken(purchaseToken)
                    .originalTransactionId(purchaseToken)
                    .periodoFimEm(periodoFim)
                    .autoRenovacao(!STATE_CANCELED.equals(purchase.getSubscriptionState()))
                    .build();
        } catch (Exception e) {
            log.error("Erro ao consultar assinatura Google: {}", e.getMessage());
            throw new CustomError("Falha ao validar assinatura no Google Play", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void acknowledge(PlataformaAssinaturaEnum plataforma, String purchaseToken, String productId) {
        try {
            androidPublisher.purchases().subscriptions()
                    .acknowledge(
                            assinaturaProperties.getGoogle().getPackageName(),
                            productId,
                            purchaseToken,
                            null
                    )
                    .execute();
        } catch (Exception e) {
            log.warn("Falha ao acknowledge assinatura Google (pode já estar confirmada): {}", e.getMessage());
        }
    }

    private LocalDateTime extractExpiry(SubscriptionPurchaseV2 purchase) {
        if (purchase.getLineItems() == null || purchase.getLineItems().isEmpty()) {
            return null;
        }
        SubscriptionPurchaseLineItem lineItem = purchase.getLineItems().getFirst();
        if (lineItem.getExpiryTime() == null) {
            return null;
        }
        Instant instant = Instant.parse(lineItem.getExpiryTime());
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private StatusAssinaturaEnum mapGoogleStatus(String subscriptionState, LocalDateTime periodoFim) {
        if (STATE_ACTIVE.equals(subscriptionState) || STATE_IN_GRACE.equals(subscriptionState)) {
            return StatusAssinaturaEnum.ATIVA;
        }
        if (STATE_ON_HOLD.equals(subscriptionState)) {
            return StatusAssinaturaEnum.EM_ATRASO;
        }
        if (periodoFim != null && periodoFim.isAfter(LocalDateTime.now())) {
            return StatusAssinaturaEnum.ATIVA;
        }
        return StatusAssinaturaEnum.EXPIRADA;
    }
}
