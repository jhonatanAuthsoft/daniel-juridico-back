package com.laweact.service.imp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.laweact.config.AwsS3Properties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.arquivo.ArquivoUrlLeituraInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlLeituraResponseDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadResponseDTO;
import com.laweact.model.enums.ArquivoFinalidade;
import com.laweact.service.S3StorageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArquivoService — URLs assinadas")
class ArquivoServiceImpTest {

    @Mock
    private S3StorageService s3StorageService;

    private ArquivoServiceImp service;

    @BeforeEach
    void setUp() {
        AwsS3Properties props = new AwsS3Properties();
        props.setEnabled(true);
        props.setUploadUrlExpirationSeconds(900);
        props.setReadUrlExpirationSeconds(900);
        props.setMaxContentLengthBytes(25L * 1024 * 1024);
        service = new ArquivoServiceImp(s3StorageService, props);
    }

    @Test
    @DisplayName("url-upload gera key no prefixo da finalidade e retorna URL de PUT")
    void shouldCreateUploadUrl() {
        when(s3StorageService.gerarUrlUpload(any(), eq("image/jpeg"), eq(Duration.ofSeconds(900))))
                .thenReturn(URI.create("https://s3.example/upload"));

        ArquivoUrlUploadResponseDTO response = service.criarUrlUpload(
                ArquivoUrlUploadInputDTO.builder()
                        .finalidade(ArquivoFinalidade.ADVOGADO_PERFIL)
                        .contentType("image/jpeg")
                        .build()
        );

        assertThat(response.key()).startsWith("tmp/advogados/perfil/");
        assertThat(response.key()).endsWith(".jpg");
        assertThat(response.uploadUrl()).isEqualTo("https://s3.example/upload");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "image/jpeg");
        verify(s3StorageService).gerarUrlUpload(eq(response.key()), eq("image/jpeg"), eq(Duration.ofSeconds(900)));
    }

    @Test
    @DisplayName("url-upload rejeita contentType inválido")
    void shouldRejectInvalidContentType() {
        assertThatThrownBy(() -> service.criarUrlUpload(
                ArquivoUrlUploadInputDTO.builder()
                        .finalidade(ArquivoFinalidade.CLIENTE_PERFIL)
                        .contentType("application/pdf")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> assertThat(((CustomError) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("url-upload rejeita arquivo maior que o limite")
    void shouldRejectOversizedContent() {
        assertThatThrownBy(() -> service.criarUrlUpload(
                ArquivoUrlUploadInputDTO.builder()
                        .finalidade(ArquivoFinalidade.OAB)
                        .contentType("image/png")
                        .contentLength(26L * 1024 * 1024)
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> assertThat(((CustomError) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("url-leitura assina key válida")
    void shouldCreateReadUrl() {
        String key = "tmp/clientes/perfil/11111111-1111-1111-1111-111111111111.jpg";
        when(s3StorageService.gerarUrlLeitura(eq(key), eq(Duration.ofSeconds(900))))
                .thenReturn(URI.create("https://s3.example/read"));

        ArquivoUrlLeituraResponseDTO response = service.criarUrlLeitura(
                ArquivoUrlLeituraInputDTO.builder().key(key).build()
        );

        assertThat(response.key()).isEqualTo(key);
        assertThat(response.readUrl()).isEqualTo("https://s3.example/read");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("url-leitura rejeita key fora do padrão")
    void shouldRejectInvalidKey() {
        assertThatThrownBy(() -> service.criarUrlLeitura(
                ArquivoUrlLeituraInputDTO.builder().key("../etc/passwd").build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> assertThat(((CustomError) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("valida key da finalidade do perfil")
    void shouldAcceptKeyForMatchingFinalidade() {
        service.validarKeyParaFinalidade(
                "tmp/clientes/perfil/11111111-1111-1111-1111-111111111111.jpg",
                ArquivoFinalidade.CLIENTE_PERFIL
        );
    }

    @Test
    @DisplayName("rejeita key de outra finalidade")
    void shouldRejectKeyForOtherFinalidade() {
        assertThatThrownBy(() -> service.validarKeyParaFinalidade(
                "tmp/advogados/perfil/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png",
                ArquivoFinalidade.CLIENTE_PERFIL
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError err = (CustomError) ex;
                    assertThat(err.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(err.getErrorCode()).isEqualTo("INVALID_OBJECT_KEY");
                });
    }

    @Test
    @DisplayName("rejeita key fora do padrão na validação de finalidade")
    void shouldRejectMalformedKeyForFinalidade() {
        assertThatThrownBy(() -> service.validarKeyParaFinalidade(
                "tmp/clientes/perfil/joao.jpg",
                ArquivoFinalidade.CLIENTE_PERFIL
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> assertThat(((CustomError) ex).getErrorCode()).isEqualTo("INVALID_OBJECT_KEY"));
    }

    @Test
    @DisplayName("falha com 503 quando S3 está desabilitado")
    void shouldFailWhenS3Disabled() {
        AwsS3Properties props = new AwsS3Properties();
        props.setEnabled(false);
        service = new ArquivoServiceImp(s3StorageService, props);

        assertThatThrownBy(() -> service.criarUrlUpload(
                ArquivoUrlUploadInputDTO.builder()
                        .finalidade(ArquivoFinalidade.CLIENTE_PERFIL)
                        .contentType("image/jpeg")
                        .build()
        ))
                .isInstanceOf(CustomError.class)
                .satisfies(ex -> {
                    CustomError err = (CustomError) ex;
                    assertThat(err.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(err.getErrorCode()).isEqualTo("S3_DISABLED");
                });
    }
}
