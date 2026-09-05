package com.laweact.service;

import com.laweact.dto.assinatura.AppleNotificationInputDTO;
import com.laweact.dto.assinatura.GooglePubSubNotificationInputDTO;

public interface AssinaturaNotificationService {

    void processarApple(AppleNotificationInputDTO input);

    void processarGoogle(GooglePubSubNotificationInputDTO input);
}
