package com.laweact.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.laweact.service.S3StorageService;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
@Profile("local")
public class S3StartupCheckConfig {

    @Bean
    @ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
    ApplicationRunner s3ConnectionCheck(S3StorageService s3StorageService) {
        return args -> {
            try {
                s3StorageService.verificarConexao();
            } catch (Exception ex) {
                log.error(
                        "Falha ao conectar no S3 (bucket={}): {}",
                        s3StorageService.getBucket(),
                        ex.getMessage()
                );
                throw ex;
            }
        };
    }
}
