package com.healthcare.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class AuthTransactionLock {
    private final JdbcTemplate jdbc;

    public void acquire(String resource) {
        Integer result = jdbc.queryForObject("""
                DECLARE @result INT;
                EXEC @result = sys.sp_getapplock @Resource = ?, @LockMode = 'Exclusive',
                    @LockOwner = 'Transaction', @LockTimeout = 5000;
                SELECT @result;
                """, Integer.class, "healthcare:auth:" + resource);
        if (result == null || result < 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication request is busy");
        }
    }
}