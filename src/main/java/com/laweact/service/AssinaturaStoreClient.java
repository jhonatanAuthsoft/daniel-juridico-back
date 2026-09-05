package com.laweact.service;

import com.laweact.dto.assinatura.AssinaturaStoreStateDTO;
import com.laweact.model.enums.PlataformaAssinaturaEnum;

public interface AssinaturaStoreClient {

    boolean supports(PlataformaAssinaturaEnum plataforma, String purchaseToken);

    AssinaturaStoreStateDTO consultar(
            PlataformaAssinaturaEnum plataforma,
            String purchaseToken,
            String productId
    );

    void acknowledge(PlataformaAssinaturaEnum plataforma, String purchaseToken, String productId);
}
