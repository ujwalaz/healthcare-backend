package com.healthcare.repository;

import com.healthcare.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    Page<Patient> findByNameContainingIgnoreCaseOrMobileNumberContaining(
            String name, String mobileNumber, Pageable pageable);
}
