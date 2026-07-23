package com.laweact.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.laweact.config.exception.CustomError;
import com.laweact.dto.shared.ApiError;
import com.laweact.dto.shared.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String code = "INVALID_FIELD";
                    if (error.getField().contains("email")) {
                        code = "INVALID_EMAIL";
                    } else if (error.getCode() != null && (error.getCode().equals("NotNull") || error.getCode().equals("NotBlank") || error.getCode().equals("NotEmpty"))) {
                        code = "REQUIRED_FIELD";
                    }
                    return ApiError.builder()
                            .code(code)
                            .field(error.getField())
                            .detail(error.getDefaultMessage())
                            .build();
                })
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.error(errors, "Erro de validação");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(CustomError.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomError(CustomError ex) {
        String code = ex.getErrorCode();
        if (code == null) {
            if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
                code = "RESOURCE_NOT_FOUND";
            } else if (ex.getHttpStatus() == HttpStatus.UNAUTHORIZED) {
                code = "UNAUTHORIZED";
            } else if (ex.getHttpStatus() == HttpStatus.FORBIDDEN) {
                code = "FORBIDDEN";
            } else if (ex.getHttpStatus() == HttpStatus.BAD_REQUEST) {
                code = "INVALID_REQUEST";
            } else {
                code = "INTERNAL_ERROR";
            }
        }

        ApiError error = ApiError.builder()
                .code(code)
                .detail(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(List.of(error), "Erro ao processar requisição");
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ApiError error = ApiError.builder()
                .code("INTERNAL_ERROR")
                .detail("Erro inesperado")
                .build();

        ApiResponse<Void> response = ApiResponse.error(List.of(error), "Erro interno do servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

