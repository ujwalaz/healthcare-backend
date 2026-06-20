package com.healthcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientHospitalLinkId implements Serializable {

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientHospitalLinkId that)) return false;
        return Objects.equals(patientId, that.patientId) && Objects.equals(hospitalId, that.hospitalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId, hospitalId);
    }
}
