package com.laweact.auth.presentation.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
