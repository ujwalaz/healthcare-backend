package com.healthcare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String role, Long hospitalId) {
        return generateToken(userId, role, hospitalId, 0);
    }

    public String generateToken(Long userId, String role, Long hospitalId, Integer passwordVersion) {
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("role", role)
                .claim("hospitalId", hospitalId)
                .claim("passwordVersion", passwordVersion == null ? 0 : passwordVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT presented");
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long extractHospitalId(String token) {
        Object hospitalId = getClaims(token).get("hospitalId");
        if (hospitalId == null) return null;
        if (hospitalId instanceof Integer) return ((Integer) hospitalId).longValue();
        if (hospitalId instanceof Long) return (Long) hospitalId;
        return Long.parseLong(hospitalId.toString());
    }

    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public Integer extractPasswordVersion(String token) {
        Object version = getClaims(token).get("passwordVersion");
        if (version == null) return null;
        if (version instanceof Integer) return (Integer) version;
        return Integer.parseInt(version.toString());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}