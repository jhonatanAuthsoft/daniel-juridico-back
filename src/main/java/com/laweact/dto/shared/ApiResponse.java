package com.laweact.dto.shared;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final Instant timestamp;
    private final String message;
    private final T data;
    private final PaginationInfo pagination;
    private final List<ApiError> errors;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .message(message)
                .data(data)
                .pagination(pagination)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operação realizada com sucesso");
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    public static <T> ApiResponse<T> error(List<ApiError> errors, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .timestamp(Instant.now())
                .message(message)
                .errors(errors)
                .build();
    }
}

