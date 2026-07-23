package com.laweact.dto.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private final String code;
    private final String field;
    private final String detail;

    public ApiError(String code, String field, String detail) {
        this.code = code;
        this.field = field;
        this.detail = detail;
    }
}
