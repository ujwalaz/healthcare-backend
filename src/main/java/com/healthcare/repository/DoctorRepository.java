package com.healthcare.repository;

import com.healthcare.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByHospitalId(Long hospitalId);

    Page<Doctor> findByHospitalId(Long hospitalId, Pageable pageable);

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
