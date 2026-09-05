package com.healthcare.repository;

import com.healthcare.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}