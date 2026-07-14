package com.laweact.auth.presentation.dto;

import java.util.UUID;

public record UserResponse(UUID id, String email) {
}
