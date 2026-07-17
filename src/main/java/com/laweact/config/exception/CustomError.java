package com.laweact.config.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class CustomError extends RuntimeException {
    private final HttpStatus httpStatus;

    public CustomError(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
