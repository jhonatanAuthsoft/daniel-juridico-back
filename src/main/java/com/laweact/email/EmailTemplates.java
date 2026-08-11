package com.laweact.email;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Templates HTML de e-mail (layout mobile / card branco).
 * Ícones: {@code classpath:email/assets/*.svg}
 */
public final class EmailTemplates {

    private static final String ICON_EMAIL_DATA_URI = svgDataUri("email/assets/mail.svg");
    private static final String ICON_INSTAGRAM_DATA_URI = svgDataUri("email/assets/instagram.svg");
    private static final String ICON_WHATSAPP_DATA_URI = svgDataUri("email/assets/whatsapp.svg");

    private EmailTemplates() {
    }

    public static String recuperacaoSenhaHtml(String codigo) {
        String codigoEscapado = escapar(codigo);
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Recuperação de senha</title>
                </head>
                <body style="margin:0;padding:0;background-color:#333333;-webkit-text-size-adjust:100%%;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#333333;">
                    <tr>
                      <td align="center" style="padding:24px 16px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:380px;background-color:#ffffff;border-radius:16px;">
                          <tr>
                            <td style="padding:36px 28px 32px 28px;font-family:Arial,Helvetica,sans-serif;color:#3E404D;text-align:left;">

                              <p style="margin:0 0 20px 0;font-size:22px;line-height:1.3;font-weight:700;color:#2F3340;">
                                Olá, tudo bem?
                              </p>

                              <p style="margin:0 0 16px 0;font-size:15px;line-height:1.55;font-weight:400;color:#555555;">
                                Recebemos uma solicitação de recuperação de senha para a conta cadastrada nesse e-mail. Para recuperar sua senha:
                              </p>

                              <p style="margin:0 0 6px 0;font-size:15px;line-height:1.55;color:#555555;">
                                1. Copie o código abaixo;
                              </p>
                              <p style="margin:0 0 28px 0;font-size:15px;line-height:1.55;color:#555555;">
                                2. Cole na tela de redefinição de senha do aplicativo.
                              </p>

                              <p style="margin:0 0 28px 0;font-size:40px;line-height:1.1;font-weight:700;letter-spacing:2px;color:#2F3340;">
                                %s
                              </p>

                              <p style="margin:0 0 36px 0;font-size:15px;line-height:1.55;color:#555555;">
                                Caso tenha dificuldades em seu acesso ao aplicativo, consulte nosso suporte. Será um prazer poder te ajudar!
                              </p>

                              <!-- Logo placeholder (trocar por <img> depois) -->
                              <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 28px 0;">
                                <tr>
                                  <td style="background-color:#E83428;padding:10px 18px;">
                                    <span style="font-family:Arial,Helvetica,sans-serif;font-size:20px;font-weight:700;color:#ffffff;letter-spacing:0.5px;">
                                      Laweact
                                    </span>
                                  </td>
                                </tr>
                              </table>

                              <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td style="padding:0 14px 0 0;">
                                    <a href="mailto:suporte@laweact.com" style="text-decoration:none;" title="E-mail">
                                      <img src="%s" width="24" height="24" alt="E-mail" style="display:block;border:0;">
                                    </a>
                                  </td>
                                  <td style="padding:0 14px 0 0;">
                                    <a href="https://instagram.com/laweact" style="text-decoration:none;" title="Instagram">
                                      <img src="%s" width="24" height="24" alt="Instagram" style="display:block;border:0;">
                                    </a>
                                  </td>
                                  <td style="padding:0;">
                                    <a href="https://wa.me/5500000000000" style="text-decoration:none;" title="WhatsApp">
                                      <img src="%s" width="24" height="24" alt="WhatsApp" style="display:block;border:0;">
                                    </a>
                                  </td>
                                </tr>
                              </table>

                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                codigoEscapado,
                ICON_EMAIL_DATA_URI,
                ICON_INSTAGRAM_DATA_URI,
                ICON_WHATSAPP_DATA_URI
        );
    }

    public static String recuperacaoSenhaTexto(String codigo) {
        return """
                Olá, tudo bem?

                Recebemos uma solicitação de recuperação de senha para a conta cadastrada nesse e-mail. Para recuperar sua senha:
                1. Copie o código abaixo;
                2. Cole na tela de redefinição de senha do aplicativo.

                %s

                Caso tenha dificuldades em seu acesso ao aplicativo, consulte nosso suporte. Será um prazer poder te ajudar!

                — Laweact
                """.formatted(codigo);
    }

    private static String svgDataUri(String classpathResource) {
        try (InputStream in = EmailTemplates.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Asset de e-mail não encontrado: " + classpathResource);
            }
            byte[] bytes = in.readAllBytes();
            // normaliza para UTF-8 e remove newlines desnecessárias no SVG
            String svg = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
            String base64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
            return "data:image/svg+xml;base64," + base64;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler asset de e-mail: " + classpathResource, e);
        }
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
