package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_relationship")
@Getter
@Setter
@NoArgsConstructor
public class PatientRelationship extends UpdatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_patient_id", nullable = false, updatable = false)
    private Long ownerPatientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_patient_id", nullable = false, updatable = false)
    private Patient member;

    @Column(name = "relationship_type", nullable = false)
    private String relationshipType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}