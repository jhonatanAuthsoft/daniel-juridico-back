package com.laweact.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class S3Config {

    @Bean
    @ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
    public S3Client s3Client(AwsS3Properties props) {
        validarCredenciais(props);
        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
    public S3Presigner s3Presigner(AwsS3Properties props) {
        validarCredenciais(props);
        return S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .build();
    }

    private void validarCredenciais(AwsS3Properties props) {
        if (props.getBucket() == null || props.getBucket().isBlank()) {
            throw new IllegalStateException("aws.s3.bucket não configurado");
        }
        if (props.getAccessKey() == null || props.getAccessKey().isBlank()
                || props.getSecretKey() == null || props.getSecretKey().isBlank()) {
            throw new IllegalStateException(
                    "Credenciais AWS ausentes. Configure aws.s3.access-key e aws.s3.secret-key."
            );
        }
    }
}
