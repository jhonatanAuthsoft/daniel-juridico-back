package com.laweact.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailTemplates — recuperação de senha")
class EmailTemplatesTest {

    @Test
    @DisplayName("HTML deve conter copy do mock e o código")
    void shouldRenderRecoveryTemplate() {
        String html = EmailTemplates.recuperacaoSenhaHtml("4242");

        assertThat(html)
                .contains("Olá, tudo bem?")
                .contains("Recebemos uma solicitação de recuperação de senha")
                .contains("1. Copie o código abaixo;")
                .contains("2. Cole na tela de redefinição de senha do aplicativo.")
                .contains("4242")
                .contains("Laweact")
                .contains("#E83428")
                .contains("#333333")
                .contains("data:image/svg+xml;base64,");
    }
}
