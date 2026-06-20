package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_hospital_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHospitalLink {

    @EmbeddedId
    private PatientHospitalLinkId id;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;
}
