package com.healthcare.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OtpStateRepository {
    private final JdbcTemplate jdbc;

    public record State(String purpose, String role, Long accountId, LocalDateTime expiresAt,
                        LocalDateTime resendAfter, int attempts, boolean pending) {
        public boolean matches(String requestedPurpose, String requestedRole) {
            return purpose.equals(requestedPurpose) && Objects.equals(role, requestedRole);
        }
    }

    public Optional<State> find(String mobile) {
        return jdbc.query("""
                SELECT purpose, account_role, account_id, expires_at, resend_after, attempts, pending
                FROM auth_otp_state WHERE mobile_number = ?
                """, (rs, row) -> new State(rs.getString("purpose"), rs.getString("account_role"),
                rs.getObject("account_id", Long.class), rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getTimestamp("resend_after").toLocalDateTime(), rs.getInt("attempts"),
                rs.getBoolean("pending")), mobile).stream().findFirst();
    }

    // Caller holds the transaction lock for this mobile number.
    public void replace(String mobile, String purpose, String role, Long accountId,
                        LocalDateTime expiry, LocalDateTime resendAfter) {
        if (jdbc.update("""
                UPDATE auth_otp_state SET purpose = ?, account_role = ?, account_id = ?,
                    expires_at = ?, resend_after = ?, attempts = 0, pending = 0, updated_at = SYSUTCDATETIME()
                WHERE mobile_number = ?
                """, purpose, role, accountId, expiry, resendAfter, mobile) == 1) return;
        jdbc.update("""
                INSERT INTO auth_otp_state (mobile_number, purpose, account_role, account_id,
                    expires_at, resend_after, attempts, pending)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0)
                """, mobile, purpose, role, accountId, expiry, resendAfter);
    }

    public void markSent(String mobile) {
        jdbc.update("UPDATE auth_otp_state SET pending = 1, updated_at = SYSUTCDATETIME() WHERE mobile_number = ?", mobile);
    }

    public void recordAttempt(String mobile) {
        jdbc.update("UPDATE auth_otp_state SET attempts = attempts + 1, updated_at = SYSUTCDATETIME() WHERE mobile_number = ?", mobile);
    }

    public void markVerified(String mobile, String hash, LocalDateTime expiry) {
        jdbc.update("""
                UPDATE auth_otp_state SET pending = 0, verification_hash = ?, verification_expires_at = ?,
                    consumed_at = NULL, updated_at = SYSUTCDATETIME()
                WHERE mobile_number = ?
                """, hash, expiry, mobile);
    }

    public void endPasswordResetOtp(String mobile) {
        jdbc.update("UPDATE auth_otp_state SET pending = 0, updated_at = SYSUTCDATETIME() WHERE mobile_number = ?", mobile);
    }

    public boolean consumeRegistration(String mobile, String hash, LocalDateTime now) {
        return jdbc.update("""
                UPDATE auth_otp_state SET consumed_at = ?, verification_hash = NULL, updated_at = SYSUTCDATETIME()
                WHERE mobile_number = ? AND verification_hash = ?
                    AND verification_expires_at > ? AND consumed_at IS NULL
                """, now, mobile, hash, now) == 1;
    }
}