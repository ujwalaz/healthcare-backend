package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "uploaded_by_role", nullable = false)
    private String uploadedByRole;

    @Column(name = "uploaded_by_id", nullable = false)
    private Long uploadedById;

    @Column(name = "is_visible_to_patient", nullable = false)
    private Boolean isVisibleToPatient;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
