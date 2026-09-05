package com.laweact.service.imp;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laweact.config.AssinaturaProperties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.model.enums.AmbienteAssinaturaEnum;
import com.laweact.model.enums.PlataformaAssinaturaEnum;
import com.laweact.model.enums.StatusAssinaturaEnum;
import com.laweact.service.AssinaturaStoreClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "laweact.assinatura.fake-store.enabled", havingValue = "true")
public class FakeStoreClientImp implements AssinaturaStoreClient {

    private final AssinaturaProperties assinaturaProperties;

    @Override
    public boolean supports(PlataformaAssinaturaEnum plataforma, String purchaseToken) {
        return plataforma == PlataformaAssinaturaEnum.FAKE
                || (purchaseToken != null && purchaseToken.startsWith("fake:"));
    }

    @Override
    public AssinaturaStoreStateDTO consultar(
            PlataformaAssinaturaEnum plataforma,
            String purchaseToken,
            String productId
    ) {
        if (!supports(plataforma, purchaseToken)) {
            throw new CustomError("Token fake inválido", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime periodoFim = LocalDateTime.now().plus(assinaturaProperties.getFakeStore().getActiveDuration());
        return AssinaturaStoreStateDTO.builder()
                .status(StatusAssinaturaEnum.ATIVA)
                .plataforma(PlataformaAssinaturaEnum.FAKE)
                .ambiente(AmbienteAssinaturaEnum.FAKE)
                .productId(productId)
                .purchaseToken(purchaseToken)
                .originalTransactionId(purchaseToken)
                .periodoFimEm(periodoFim)
                .autoRenovacao(true)
                .build();
    }

    @Override
    public void acknowledge(PlataformaAssinaturaEnum plataforma, String purchaseToken, String productId) {
        // noop for fake store
    }
}
