package com.healthcare.repository;

import com.healthcare.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    int countByDoctorIdAndAppointmentDateAndStatusNot(Long doctorId, Date date, String status);

    int countByDoctorIdAndAppointmentDateAndStartTimeLessThanAndStatusNot(
            Long doctorId, Date date, Time beforeTime, String status);

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, Date date);

    List<Appointment> findByAppointmentDate(Date date);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByHospitalId(Long hospitalId, Pageable pageable);

    boolean existsByDoctorIdAndAppointmentDateAndStartTime(Long doctorId, Date date, Time startTime);

    @Query(value = "SELECT a.id AS id, p.name AS patientName, d.name AS doctorName, " +
           "a.appointmentDate AS appointmentDate, a.startTime AS startTime, a.endTime AS endTime, " +
           "a.status AS status, a.isOffline AS isOffline " +
           "FROM Appointment a JOIN Patient p ON p.id = a.patientId JOIN Doctor d ON d.id = a.doctorId WHERE " +
           "(:patientId IS NULL OR a.patientId = :patientId) AND " +
           "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
           "(:hospitalId IS NULL OR a.hospitalId = :hospitalId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:date IS NULL OR a.appointmentDate = :date)",
           countQuery = "SELECT COUNT(a) FROM Appointment a WHERE " +
           "(:patientId IS NULL OR a.patientId = :patientId) AND " +
           "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
           "(:hospitalId IS NULL OR a.hospitalId = :hospitalId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:date IS NULL OR a.appointmentDate = :date)")
    Page<AppointmentSummaryProjection> findWithFilters(
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            @Param("hospitalId") Long hospitalId,
            @Param("status") String status,
            @Param("date") Date date,
            Pageable pageable);

    interface AppointmentSummaryProjection {
        Long getId();
        String getPatientName();
        String getDoctorName();
        Date getAppointmentDate();
        Time getStartTime();
        Time getEndTime();
        String getStatus();
        Boolean getIsOffline();
    }

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.patientId = :patientId " +
           "AND a.status IN ('SCHEDULED', 'COMPLETED')")
    List<Appointment> findActiveByDoctorIdAndPatientId(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId);
}
