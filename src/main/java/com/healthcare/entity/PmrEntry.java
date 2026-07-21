package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "pmr_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmrEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pmr_id", nullable = false)
    private Long pmrId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "entry_date", nullable = false)
    private Date entryDate;

    @Column(name = "diagnosis")
    private String diagnosis;

    @Column(name = "symptoms")
    private String symptoms;

    @Column(name = "treatment_plan")
    private String treatmentPlan;

    @Column(name = "doctor_notes")
    private String doctorNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}