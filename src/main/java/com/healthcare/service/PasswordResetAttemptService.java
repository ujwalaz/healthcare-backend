package com.healthcare.service;

import com.healthcare.entity.PasswordResetAttempt;
import com.healthcare.repository.PasswordResetAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    private final PasswordResetAttemptRepository repository;

    @Transactional(readOnly = true)
    public boolean isLocked(String role, Long accountId, LocalDateTime now) {
        return repository.findByAccountRoleAndAccountId(role, accountId)
                .map(attempt -> attempt.getLockedUntil() != null && attempt.getLockedUntil().isAfter(now))
                .orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String role, Long accountId, LocalDateTime now) {
        PasswordResetAttempt attempt = repository.findByAccountRoleAndAccountId(role, accountId)
                .orElseGet(() -> PasswordResetAttempt.builder()
                        .accountRole(role)
                        .accountId(accountId)
                        .failedAttempts(0)
                        .windowStartedAt(now)
                        .build());

        if (!now.isBefore(attempt.getWindowStartedAt().plusMinutes(WINDOW_MINUTES))) {
            attempt.setFailedAttempts(0);
            attempt.setWindowStartedAt(now);
            attempt.setLockedUntil(null);
        }

        int failures = attempt.getFailedAttempts() + 1;
        attempt.setFailedAttempts(failures);
        attempt.setLockedUntil(failures >= MAX_ATTEMPTS ? now.plusMinutes(WINDOW_MINUTES) : null);
        attempt.setUpdatedAt(now);
        repository.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String role, Long accountId) {
        repository.findByAccountRoleAndAccountId(role, accountId).ifPresent(repository::delete);
    }
}