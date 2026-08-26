package com.laweact.service.imp;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.laweact.config.AwsS3Properties;
import com.laweact.config.exception.CustomError;
import com.laweact.dto.arquivo.ArquivoUrlLeituraInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlLeituraResponseDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadInputDTO;
import com.laweact.dto.arquivo.ArquivoUrlUploadResponseDTO;
import com.laweact.model.enums.ArquivoFinalidade;
import com.laweact.service.ArquivoService;
import com.laweact.service.S3StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArquivoServiceImp implements ArquivoService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private static final Pattern KEY_PERMITIDA = Pattern.compile(
            "^(tmp/)?(clientes/perfil|advogados/perfil|advogados/oab)/"
                    + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
                    + "\\.(jpg|jpeg|png)$"
    );

    private final S3StorageService s3StorageService;
    private final AwsS3Properties properties;

    @Override
    public ArquivoUrlUploadResponseDTO criarUrlUpload(ArquivoUrlUploadInputDTO input) {
        garantirS3Habilitado();

        String contentType = normalizarContentType(input.contentType());
        if (!CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new CustomError(
                    "contentType inválido. Use image/jpeg ou image/png",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CONTENT_TYPE"
            );
        }

        if (input.contentLength() != null && input.contentLength() > properties.getMaxContentLengthBytes()) {
            throw new CustomError(
                    "Arquivo excede o tamanho máximo permitido",
                    HttpStatus.BAD_REQUEST,
                    "FILE_TOO_LARGE"
            );
        }

        String key = gerarKey(input.finalidade(), contentType);
        Duration expiracao = Duration.ofSeconds(properties.getUploadUrlExpirationSeconds());
        URI uploadUrl = s3StorageService.gerarUrlUpload(key, contentType, expiracao);

        return ArquivoUrlUploadResponseDTO.builder()
                .key(key)
                .uploadUrl(uploadUrl.toString())
                .expiresInSeconds(properties.getUploadUrlExpirationSeconds())
                .requiredHeaders(Map.of("Content-Type", contentType))
                .build();
    }

    @Override
    public ArquivoUrlLeituraResponseDTO criarUrlLeitura(ArquivoUrlLeituraInputDTO input) {
        String key = input.key().trim();
        if (!KEY_PERMITIDA.matcher(key).matches()) {
            throw new CustomError("key inválida", HttpStatus.BAD_REQUEST, "INVALID_OBJECT_KEY");
        }

        garantirS3Habilitado();

        Duration expiracao = Duration.ofSeconds(properties.getReadUrlExpirationSeconds());
        URI readUrl = s3StorageService.gerarUrlLeitura(key, expiracao);

        return ArquivoUrlLeituraResponseDTO.builder()
                .key(key)
                .readUrl(readUrl.toString())
                .expiresInSeconds(properties.getReadUrlExpirationSeconds())
                .build();
    }

    @Override
    public void validarKeyParaFinalidade(String key, ArquivoFinalidade finalidade) {
        String normalized = key == null ? "" : key.trim();
        if (!KEY_PERMITIDA.matcher(normalized).matches()) {
            throw new CustomError("key inválida", HttpStatus.BAD_REQUEST, "INVALID_OBJECT_KEY");
        }

        String prefixo = finalidade.getPrefixo() + "/";
        String prefixoSemTmp = prefixo.startsWith("tmp/") ? prefixo.substring(4) : prefixo;
        if (!normalized.startsWith(prefixo) && !normalized.startsWith(prefixoSemTmp)) {
            throw new CustomError(
                    "A foto de perfil não corresponde ao tipo de usuário",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_OBJECT_KEY"
            );
        }
    }

    static String gerarKey(ArquivoFinalidade finalidade, String contentType) {
        String extensao = contentType.equals("image/png") ? "png" : "jpg";
        return finalidade.getPrefixo() + "/" + UUID.randomUUID() + "." + extensao;
    }

    private void garantirS3Habilitado() {
        if (!properties.isEnabled()) {
            throw new CustomError(
                    "Armazenamento de arquivos indisponível",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3_DISABLED"
            );
        }
    }

    private static String normalizarContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
