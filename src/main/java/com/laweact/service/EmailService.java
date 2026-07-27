package com.laweact.service;

public interface EmailService {

    void enviarTexto(String destinatario, String assunto, String corpo);
}
