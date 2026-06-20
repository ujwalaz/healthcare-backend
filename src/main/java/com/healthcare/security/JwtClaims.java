package com.healthcare.security;

public record JwtClaims(Long userId, String role, Long hospitalId) {
}
