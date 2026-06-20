package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_pmr_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPmrAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "access_granted_at", nullable = false, updatable = false)
    private LocalDateTime accessGrantedAt;

    @Column(name = "access_expires_at", nullable = false)
    private LocalDateTime accessExpiresAt;
}
