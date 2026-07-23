package com.laweact.config.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class CustomError extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public CustomError(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = null;
    }

    public CustomError(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}

