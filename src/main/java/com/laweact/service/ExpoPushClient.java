package com.laweact.service;

import java.util.Map;
import java.util.UUID;

import com.laweact.model.enums.TipoNotificacaoEnum;

public interface ExpoPushClient {

    ExpoPushSendResult enviar(
            String expoPushToken,
            String titulo,
            String texto,
            TipoNotificacaoEnum tipo,
            UUID referenciaId
    );

    sealed interface ExpoPushSendResult {
        record Success() implements ExpoPushSendResult {}

        record DeviceNotRegistered() implements ExpoPushSendResult {}

        record Error(String message) implements ExpoPushSendResult {}
    }

    record ExpoPushPayload(
            String to,
            String title,
            String body,
            Map<String, Object> data
    ) {}
}
