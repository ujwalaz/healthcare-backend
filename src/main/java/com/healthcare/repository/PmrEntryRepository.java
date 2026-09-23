package com.healthcare.repository;

import com.healthcare.entity.PmrEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;

@Repository
public interface PmrEntryRepository extends JpaRepository<PmrEntry, Long> {

    Page<PmrEntry> findByPmrId(Long pmrId, Pageable pageable);

    @Query(value = "SELECT e.id AS id, e.doctorId AS doctorId, d.name AS doctorName, " +
            "e.appointmentId AS appointmentId, e.entryDate AS entryDate, e.diagnosis AS diagnosis, " +
            "e.symptoms AS symptoms, e.treatmentPlan AS treatmentPlan, e.doctorNotes AS doctorNotes, " +
            "e.createdAt AS createdAt FROM PmrEntry e JOIN Doctor d ON d.id = e.doctorId " +
            "WHERE e.pmrId = :pmrId",
            countQuery = "SELECT COUNT(e) FROM PmrEntry e WHERE e.pmrId = :pmrId")
    Page<PmrEntryProjection> findProjectedByPmrId(@Param("pmrId") Long pmrId, Pageable pageable);

    interface PmrEntryProjection {
        Long getId();
        Long getDoctorId();
        String getDoctorName();
        Long getAppointmentId();
        Date getEntryDate();
        String getDiagnosis();
        String getSymptoms();
        String getTreatmentPlan();
        String getDoctorNotes();
        java.time.LocalDateTime getCreatedAt();
    }
}
