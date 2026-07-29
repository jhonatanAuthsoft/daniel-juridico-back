package com.laweact.service;

import java.net.URI;
import java.time.Duration;

public interface S3StorageService {

    String getBucket();

    /** Verifica se o bucket existe e as credenciais têm acesso (HeadBucket). */
    void verificarConexao();

    URI gerarUrlUpload(String key, String contentType, Duration expiracao);

    URI gerarUrlLeitura(String key, Duration expiracao);
}
