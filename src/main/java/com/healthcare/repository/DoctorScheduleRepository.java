package com.healthcare.repository;

import com.healthcare.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(Long doctorId);

    Optional<DoctorSchedule> findByDoctorIdAndDayOfWeek(Long doctorId, Integer dayOfWeek);
}
