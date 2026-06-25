package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Convert(converter = com.healthcare.config.LocalTimeConverter.class)
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Convert(converter = com.healthcare.config.LocalTimeConverter.class)
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
