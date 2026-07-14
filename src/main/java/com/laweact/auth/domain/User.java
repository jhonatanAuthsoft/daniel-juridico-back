package com.laweact.auth.domain;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;

    public User(UUID id, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static User create(String email, String passwordHash) {
        return new User(UUID.randomUUID(), email.toLowerCase().trim(), passwordHash, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
