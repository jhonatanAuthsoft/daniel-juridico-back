package com.laweact.service.imp;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.model.enums.ReferenciaNotificacaoEnum;
import com.laweact.model.enums.TipoNotificacaoEnum;
import com.laweact.service.ExpoPushClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushClientImp implements ExpoPushClient {

    private final RestClient expoPushRestClient;

    @Override
    public ExpoPushSendResult enviar(
            String expoPushToken,
            String titulo,
            String texto,
            TipoNotificacaoEnum tipo,
            UUID referenciaId
    ) {
        ExpoPushPayload payload = new ExpoPushPayload(
                expoPushToken,
                titulo,
                texto,
                Map.of(
                        "tipo", tipo.name(),
                        "referenciaTipo", ReferenciaNotificacaoEnum.CONEXAO.name(),
                        "referenciaId", referenciaId.toString()
                )
        );

        try {
            JsonNode response = expoPushRestClient.post()
                    .uri("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(payload))
                    .retrieve()
                    .body(JsonNode.class);

            return interpretarResposta(response);
        } catch (RestClientException ex) {
            log.warn("Falha HTTP ao enviar push Expo: {}", ex.getMessage());
            return new ExpoPushSendResult.Error(truncar(ex.getMessage()));
        } catch (Exception ex) {
            log.warn("Erro inesperado ao enviar push Expo", ex);
            return new ExpoPushSendResult.Error(truncar(ex.getMessage()));
        }
    }

    private ExpoPushSendResult interpretarResposta(JsonNode response) {
        if (response == null) {
            return new ExpoPushSendResult.Error("Resposta vazia da Expo Push API");
        }

        JsonNode data = response.path("data");
        JsonNode ticket = data.isArray() && !data.isEmpty() ? data.get(0) : data;
        if (ticket == null || ticket.isMissingNode() || ticket.isNull()) {
            return new ExpoPushSendResult.Error("Ticket ausente na resposta Expo");
        }

        String status = ticket.path("status").asText("");
        if ("ok".equalsIgnoreCase(status)) {
            return new ExpoPushSendResult.Success();
        }

        String errorCode = ticket.path("details").path("error").asText("");
        if ("DeviceNotRegistered".equals(errorCode)) {
            return new ExpoPushSendResult.DeviceNotRegistered();
        }

        String message = ticket.path("message").asText(null);
        if (message == null || message.isBlank()) {
            message = errorCode.isBlank() ? "Erro desconhecido no envio Expo" : errorCode;
        }
        return new ExpoPushSendResult.Error(truncar(message));
    }

    private static String truncar(String message) {
        if (message == null) {
            return "Erro desconhecido";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
