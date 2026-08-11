package com.laweact.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {

    /** Quando false, o S3Client não é criado (útil em testes sem AWS). */
    private boolean enabled = false;

    private String bucket;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;

    /** TTL da URL de upload (PUT). */
    private long uploadUrlExpirationSeconds = 900;

    /** TTL da URL de leitura (GET). */
    private long readUrlExpirationSeconds = 900;

    /** Limite de tamanho aceito na solicitação de upload (bytes). Default 25 MB. */
    private long maxContentLengthBytes = 25L * 1024 * 1024;
}
