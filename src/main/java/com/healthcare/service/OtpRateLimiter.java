package com.healthcare.service;

import com.healthcare.repository.AuthTransactionLock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpRateLimiter {
    private final AuthTransactionLock lock;
    private final JdbcTemplate jdbc;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void check(String operation, String mobile, String ip, int mobileLimit, int ipLimit) {
        LocalDateTime now = LocalDateTime.now();
        hit(operation + ":ip:" + ip, ipLimit, now);
        hit(operation + ":mobile:" + mobile, mobileLimit, now);
    }

    private void hit(String key, int limit, LocalDateTime now) {
        lock.acquire("limit:" + key);
        int updated = jdbc.update("""
                UPDATE auth_otp_limits
                SET attempts = CASE WHEN window_start <= ? THEN 1 ELSE attempts + 1 END,
                    window_start = CASE WHEN window_start <= ? THEN ? ELSE window_start END,
                    updated_at = SYSUTCDATETIME()
                WHERE rate_key = ? AND (attempts < ? OR window_start <= ?)
                """, now.minusMinutes(15), now.minusMinutes(15), now, key, limit, now.minusMinutes(15));
        if (updated == 1) return;
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT CASE WHEN EXISTS (SELECT 1 FROM auth_otp_limits WHERE rate_key = ?) THEN 1 ELSE 0 END
                """, Boolean.class, key))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP requests");
        }
        jdbc.update("INSERT INTO auth_otp_limits (rate_key, window_start, attempts) VALUES (?, ?, 1)", key, now);
    }
}