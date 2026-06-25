package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false, columnDefinition = "time")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "time")
    private LocalTime endTime;

    @Column(name = "booked_by_role", nullable = false)
    private String bookedByRole;

    @Column(name = "booked_by_id", nullable = false)
    private Long bookedById;

    @Column(name = "is_offline", nullable = false)
    private Boolean isOffline;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
