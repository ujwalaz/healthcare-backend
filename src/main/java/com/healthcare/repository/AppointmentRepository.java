package com.healthcare.repository;

import com.healthcare.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByHospitalId(Long hospitalId, Pageable pageable);

    boolean existsByDoctorIdAndAppointmentDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);

    @Query("SELECT a FROM Appointment a WHERE " +
           "(:patientId IS NULL OR a.patientId = :patientId) AND " +
           "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
           "(:hospitalId IS NULL OR a.hospitalId = :hospitalId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:date IS NULL OR a.appointmentDate = :date)")
    Page<Appointment> findWithFilters(
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            @Param("hospitalId") Long hospitalId,
            @Param("status") String status,
            @Param("date") LocalDate date,
            Pageable pageable);
}
