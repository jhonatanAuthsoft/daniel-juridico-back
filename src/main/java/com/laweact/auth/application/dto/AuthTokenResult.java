package com.laweact.auth.application.dto;

public record AuthTokenResult(String accessToken, String tokenType, long expiresIn) {
}
