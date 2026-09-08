package com.laweact.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.laweact.service.AssinaturaStoreClient;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class AssinaturaStoreStartupLogger {

    public AssinaturaStoreStartupLogger(List<AssinaturaStoreClient> storeClients) {
        log.info(
                "Adaptadores de loja ativos: {}",
                storeClients.stream().map(client -> client.getClass().getSimpleName()).toList()
        );
    }
}
