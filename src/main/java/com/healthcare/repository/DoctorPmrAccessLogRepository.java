package com.healthcare.repository;

import com.healthcare.entity.DoctorPmrAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DoctorPmrAccessLogRepository extends JpaRepository<DoctorPmrAccessLog, Long> {

    Optional<DoctorPmrAccessLog> findFirstByDoctorIdAndPatientIdAndAccessExpiresAtAfter(
            Long doctorId, Long patientId, LocalDateTime now);
}
