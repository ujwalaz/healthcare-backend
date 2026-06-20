package com.healthcare.repository;

import com.healthcare.entity.PatientMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientMedicalRecordRepository extends JpaRepository<PatientMedicalRecord, Long> {

    Optional<PatientMedicalRecord> findByPatientIdAndHospitalId(Long patientId, Long hospitalId);
}
