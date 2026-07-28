package com.laweact.service.imp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.laweact.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class EmailServiceImp implements EmailService {

    private static final int MAX_TENTATIVAS = 3;

    private final JavaMailSender mailSender;
    private final boolean smtpHabilitado;
    private final boolean capturarCodigos;
    private final String from;
    private final Map<String, String> codigosCapturados = new ConcurrentHashMap<>();

    public EmailServiceImp(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${laweact.mail.from:noreply@laweact.local}") String from,
            @Value("${laweact.mail.capture-codes:false}") boolean capturarCodigos
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.smtpHabilitado = mailSender != null && mailHost != null && !mailHost.isBlank();
        this.from = from;
        this.capturarCodigos = capturarCodigos;
    }

    @Override
    public void enviarTexto(String destinatario, String assunto, String corpo) {
        if (capturarCodigos) {
            capturarSeCodigoRecuperacao(destinatario, corpo);
        }
        enviarComRetry(destinatario, assunto, null, corpo);
    }

    @Override
    public void enviarHtml(String destinatario, String assunto, String html, String textoAlternativo) {
        if (capturarCodigos) {
            capturarSeCodigoRecuperacao(destinatario, textoAlternativo != null ? textoAlternativo : html);
        }
        enviarComRetry(destinatario, assunto, html, textoAlternativo);
    }

    private void enviarComRetry(String destinatario, String assunto, String html, String texto) {
        RuntimeException ultimoErro = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                if (!smtpHabilitado) {
                    log.info(
                            "[email-mock] to={} subject={} html={} body={}",
                            destinatario,
                            assunto,
                            html != null,
                            texto
                    );
                    return;
                }

                if (html != null) {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(from);
                    helper.setTo(destinatario);
                    helper.setSubject(assunto);
                    helper.setText(texto != null ? texto : "", html);
                    mailSender.send(message);
                } else {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(from);
                    message.setTo(destinatario);
                    message.setSubject(assunto);
                    message.setText(texto);
                    mailSender.send(message);
                }

                log.info("E-mail enviado para {} (tentativa {})", destinatario, tentativa);
                return;
            } catch (RuntimeException | jakarta.mail.MessagingException ex) {
                ultimoErro = ex instanceof RuntimeException runtime
                        ? runtime
                        : new IllegalStateException(ex);
                log.warn(
                        "Falha ao enviar e-mail para {} (tentativa {}/{}): {}",
                        destinatario,
                        tentativa,
                        MAX_TENTATIVAS,
                        ex.getMessage()
                );
                sleepBackoff(tentativa);
            }
        }

        throw new IllegalStateException("Falha ao enviar e-mail após retries", ultimoErro);
    }

    public String obterCodigoCapturado(String email) {
        return codigosCapturados.get(email.toLowerCase().trim());
    }

    private void capturarSeCodigoRecuperacao(String destinatario, String corpo) {
        if (corpo == null) {
            return;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{4})\\b").matcher(corpo);
        if (matcher.find()) {
            codigosCapturados.put(destinatario.toLowerCase().trim(), matcher.group(1));
        }
    }

    private void sleepBackoff(int tentativa) {
        try {
            Thread.sleep(100L * tentativa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
