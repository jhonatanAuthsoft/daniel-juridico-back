package com.laweact.config;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class GoogleAssinaturaEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AssinaturaProperties.Google google = Binder.get(context.getEnvironment())
                .bind("laweact.assinatura.google", AssinaturaProperties.Google.class)
                .orElseGet(AssinaturaProperties.Google::new);
        if (AssinaturaStoreCredentials.isGoogleReady(google)) {
            return ConditionOutcome.match("Google IAP habilitado com service account");
        }
        return ConditionOutcome.noMatch("Google IAP desabilitado ou sem service-account-json");
    }
}
