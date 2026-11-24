package com.healthcare.repository;

import com.healthcare.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
            String tokenHash, LocalDateTime now);

    @Modifying
    @Query("update PasswordResetToken t set t.consumedAt = :now, t.updatedAt = :now " +
           "where t.id = :id and t.consumedAt is null and t.expiresAt > :now")
    int consume(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken t set t.consumedAt = :now, t.updatedAt = :now " +
           "where t.accountRole = :role and t.accountId = :accountId and t.consumedAt is null")
    int revokeForAccount(@Param("role") String role, @Param("accountId") Long accountId,
                         @Param("now") LocalDateTime now);
}