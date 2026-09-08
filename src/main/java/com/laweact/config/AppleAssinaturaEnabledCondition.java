package com.laweact.config;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AppleAssinaturaEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AssinaturaProperties.Apple apple = Binder.get(context.getEnvironment())
                .bind("laweact.assinatura.apple", AssinaturaProperties.Apple.class)
                .orElseGet(AssinaturaProperties.Apple::new);
        if (AssinaturaStoreCredentials.isAppleReady(apple)) {
            return ConditionOutcome.match("Apple IAP habilitado com credenciais");
        }
        return ConditionOutcome.noMatch("Apple IAP desabilitado ou sem issuer/key/PEM");
    }
}
