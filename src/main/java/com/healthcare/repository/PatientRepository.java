package com.healthcare.repository;

import com.healthcare.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMobileNumber(String mobileNumber);

    Optional<Patient> findByMobileNumberAndIsDeletedFalse(String mobileNumber);

    Optional<Patient> findByIdAndIsDeletedFalse(Long id);

    boolean existsByMobileNumber(String mobileNumber);

    @Query("SELECT p FROM Patient p WHERE p.isDeleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR p.mobileNumber LIKE CONCAT('%', :mobile, '%'))")
    Page<Patient> findActiveByNameOrMobile(@Param("name") String name,
                                           @Param("mobile") String mobile,
                                           Pageable pageable);

    @Query("SELECT p FROM Patient p JOIN PatientHospitalLink l ON l.id.patientId = p.id " +
           "WHERE p.isDeleted = false AND l.id.hospitalId = :hospitalId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR p.mobileNumber LIKE CONCAT('%', :query, '%'))")
    Page<Patient> searchByHospital(@Param("hospitalId") Long hospitalId,
                                   @Param("query") String query,
                                   Pageable pageable);
}