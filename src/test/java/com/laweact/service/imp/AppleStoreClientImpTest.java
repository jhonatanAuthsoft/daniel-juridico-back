package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apple.itunes.storekit.client.AppStoreServerAPIClient;
import com.apple.itunes.storekit.model.AutoRenewStatus;
import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSRenewalInfoDecodedPayload;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.LastTransactionsItem;
import com.apple.itunes.storekit.model.Status;
import com.apple.itunes.storekit.model.StatusResponse;
import com.apple.itunes.storekit.model.SubscriptionGroupIdentifierItem;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppleStoreClientImp — recibo sandbox / TestFlight")
class AppleStoreClientImpTest {

    @Mock
    private AppStoreServerAPIClient appStoreServerAPIClient;

    @Mock
    private SignedDataVerifier signedDataVerifier;

    @InjectMocks
    private AppleStoreClientImp client;

    @Test
    @DisplayName("aceita iOS real e recusa token fake")
    void shouldSupportRealIosTokensOnly() {
        assertThat(client.supports(PlataformaAssinaturaEnum.IOS, "aaa.bbb.ccc")).isTrue();
        assertThat(client.supports(PlataformaAssinaturaEnum.IOS, "fake:123")).isFalse();
        assertThat(client.supports(PlataformaAssinaturaEnum.ANDROID, "aaa.bbb.ccc")).isFalse();
    }

    @Test
    @DisplayName("JWS do sandbox com renovação desligada vira CANCELADA até o fim do período")
    void shouldReadAutoRenewOffFromSignedTransaction() throws Exception {
        String jws = "header.payload.signature";
        long expires = Instant.now().plus(20, ChronoUnit.DAYS).toEpochMilli();

        JWSTransactionDecodedPayload payload = new JWSTransactionDecodedPayload();
        payload.setProductId("laweact_basic_mensal");
        payload.setOriginalTransactionId("orig-sandbox-1");
        payload.setExpiresDate(expires);
        payload.setEnvironment(Environment.SANDBOX);

        when(signedDataVerifier.verifyAndDecodeTransaction(jws)).thenReturn(payload);

        LastTransactionsItem last = new LastTransactionsItem()
                .status(Status.ACTIVE)
                .signedTransactionInfo("signed.tx.info")
                .signedRenewalInfo("signed.renewal.info");

        SubscriptionGroupIdentifierItem group = new SubscriptionGroupIdentifierItem();
        group.setLastTransactions(List.of(last));

        StatusResponse statuses = new StatusResponse();
        statuses.setData(List.of(group));

        when(appStoreServerAPIClient.getAllSubscriptionStatuses(eq("orig-sandbox-1"), any(Status[].class)))
                .thenReturn(statuses);
        when(signedDataVerifier.verifyAndDecodeTransaction("signed.tx.info")).thenReturn(payload);

        JWSRenewalInfoDecodedPayload renewal = new JWSRenewalInfoDecodedPayload();
        renewal.setAutoRenewStatus(AutoRenewStatus.OFF);
        when(signedDataVerifier.verifyAndDecodeRenewalInfo("signed.renewal.info")).thenReturn(renewal);

        AssinaturaStoreStateDTO state = client.consultar(
                PlataformaAssinaturaEnum.IOS,
                jws,
                "laweact_basic_mensal"
        );

        assertThat(state.status()).isEqualTo(StatusAssinaturaEnum.CANCELADA);
        assertThat(state.autoRenovacao()).isFalse();
        assertThat(state.originalTransactionId()).isEqualTo("orig-sandbox-1");
        assertThat(state.purchaseToken()).isEqualTo(jws);
    }
}
