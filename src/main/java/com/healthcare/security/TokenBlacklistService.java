package com.healthcare.security;

import com.healthcare.entity.RevokedToken;
import com.healthcare.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Tracks revoked (logged-out) JWTs by their "jti" claim until natural expiry,
 * so a token cannot be reused after logout even though JWTs are stateless.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(String jti, Date expiration) {
        if (jti == null || revokedTokenRepository.existsById(jti)) {
            return;
        }
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
        revokedTokenRepository.save(RevokedToken.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .revokedAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    /**
     * Periodically purge revoked-token records once their token would have
     * expired naturally anyway, to keep the table small.
     */
    @Scheduled(fixedDelayString = "${app.jwt.revoked-cleanup-interval-ms:3600000}")
    @Transactional
    public void purgeExpired() {
        int removed = revokedTokenRepository.deleteAllExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Purged {} expired revoked-token record(s)", removed);
        }
    }
}