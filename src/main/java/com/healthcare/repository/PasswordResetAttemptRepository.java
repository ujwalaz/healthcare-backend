package com.healthcare.repository;

import com.healthcare.entity.PasswordResetAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetAttemptRepository extends JpaRepository<PasswordResetAttempt, Long> {

    Optional<PasswordResetAttempt> findByAccountRoleAndAccountId(String accountRole, Long accountId);
}