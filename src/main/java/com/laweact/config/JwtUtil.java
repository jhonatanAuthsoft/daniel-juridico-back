package com.laweact.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Log4j2
public class JwtUtil {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secretKey;

    /** Access token: 1 hora por padrão. */
    @Value("${jwt.access-token-expiration-ms:3600000}")
    private long accessTokenExpirationMs;

    /** Refresh token: 7 dias por padrão. */
    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrai claims mesmo se o JWT estiver expirado (útil no refresh do access token).
     */
    public Claims extractAllClaimsAllowExpired(String token) {
        try {
            return extractAllClaims(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaimsAllowExpired(token);
            return TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaimsAllowExpired(token);
            String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
            return typ == null || TYPE_ACCESS.equals(typ);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateAccessToken(UserDetails userDetails) {
        return createToken(buildRoleClaims(userDetails), userDetails.getUsername(), TYPE_ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(buildRoleClaims(userDetails), userDetails.getUsername(), TYPE_REFRESH, refreshTokenExpirationMs);
    }

    /** @deprecated use {@link #generateAccessToken(UserDetails)} */
    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    private Map<String, Object> buildRoleClaims(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String role = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Usuário não possui nenhuma role configurada!"));
        claims.put("role", role);
        return claims;
    }

    private String createToken(Map<String, Object> claims, String subject, String tokenType, long expirationMs) {
        Map<String, Object> allClaims = new HashMap<>(claims);
        allClaims.put(CLAIM_TOKEN_TYPE, tokenType);
        allClaims.put("jti", UUID.randomUUID().toString());
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(allClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && isAccessToken(token);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public static String getEmailFromJwtToken(String token, String jwtSecret) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public static String getLoggedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
