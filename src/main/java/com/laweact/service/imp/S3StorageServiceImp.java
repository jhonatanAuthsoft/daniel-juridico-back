package com.laweact.service.imp;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laweact.config.AwsS3Properties;
import com.laweact.config.exception.CustomError;
import com.laweact.service.S3StorageService;

import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Log4j2
public class S3StorageServiceImp implements S3StorageService {

    private final AwsS3Properties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageServiceImp(
            AwsS3Properties properties,
            ObjectProvider<S3Client> s3ClientProvider,
            ObjectProvider<S3Presigner> s3PresignerProvider
    ) {
        this.properties = properties;
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.s3Presigner = s3PresignerProvider.getIfAvailable();
    }

    @Override
    public String getBucket() {
        return properties.getBucket();
    }

    @Override
    public void verificarConexao() {
        garantirCliente();
        s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        log.info("S3 OK — bucket={} region={}", properties.getBucket(), properties.getRegion());
    }

    @Override
    public URI gerarUrlUpload(String key, String contentType, Duration expiracao) {
        garantirPresigner();
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiracao)
                .putObjectRequest(objectRequest)
                .build();

        return URI.create(s3Presigner.presignPutObject(presignRequest).url().toExternalForm());
    }

    @Override
    public URI gerarUrlLeitura(String key, Duration expiracao) {
        garantirPresigner();
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiracao)
                .getObjectRequest(objectRequest)
                .build();

        return URI.create(s3Presigner.presignGetObject(presignRequest).url().toExternalForm());
    }

    private void garantirCliente() {
        if (!properties.isEnabled() || s3Client == null) {
            throw new CustomError(
                    "Armazenamento de arquivos indisponível",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3_DISABLED"
            );
        }
    }

    private void garantirPresigner() {
        if (!properties.isEnabled() || s3Presigner == null) {
            throw new CustomError(
                    "Armazenamento de arquivos indisponível",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3_DISABLED"
            );
        }
    }
}
