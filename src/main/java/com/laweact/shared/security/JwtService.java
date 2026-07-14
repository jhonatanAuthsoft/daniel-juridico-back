package com.laweact.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + properties.getExpirationMs());
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    public long getExpirationMs() {
        return properties.getExpirationMs();
    }

    public AuthenticatedUser parseUser(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        UUID id = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        return new AuthenticatedUser(id, email);
    }
}
