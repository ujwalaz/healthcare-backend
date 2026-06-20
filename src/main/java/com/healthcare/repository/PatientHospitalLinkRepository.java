package com.healthcare.repository;

import com.healthcare.entity.PatientHospitalLink;
import com.healthcare.entity.PatientHospitalLinkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientHospitalLinkRepository extends JpaRepository<PatientHospitalLink, PatientHospitalLinkId> {
}
