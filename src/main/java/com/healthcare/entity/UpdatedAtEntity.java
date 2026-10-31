package com.healthcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Adds a consistently maintained last-modified timestamp to persisted entities.
 * Hospital and Document intentionally do not extend this class because their
 * tables are explicitly excluded from the audit-column requirement.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class UpdatedAtEntity {

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void setCreatedUpdatedTimestamp() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void setUpdatedTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}