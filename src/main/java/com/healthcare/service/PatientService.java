package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Patient;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public PatientResponse getProfile(Long patientId) {
        Patient patient = findPatientById(patientId);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse updateProfile(Long patientId, PatientUpdateRequest request) {
        Patient patient = findPatientById(patientId);

        if (StringUtils.hasText(request.getName())) {
            patient.setName(request.getName());
        }
        if (request.getDob() != null) {
            patient.setDob(Date.valueOf(request.getDob()));
        }
        if (request.getEmail() != null) {
            patient.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }
        if (request.getEmergencyContactNumber() != null) {
            patient.setEmergencyContactNumber(request.getEmergencyContactNumber());
        }
        if (request.getBloodGroup() != null) {
            patient.setBloodGroup(request.getBloodGroup().getValue());
        }
        if (request.getPermanentIllness() != null) {
            patient.setPermanentIllness(request.getPermanentIllness());
        }

        patient = patientRepository.save(patient);
        log.info("Updated profile for patientId={}", patientId);
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return toResponse(findPatientById(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PatientSummaryResponse> searchPatients(String query, Pageable pageable) {
        String searchTerm = StringUtils.hasText(query) ? query : "";
        Page<Patient> page = patientRepository
                .findByNameContainingIgnoreCaseOrMobileNumberContaining(searchTerm, searchTerm, pageable);
        Page<PatientSummaryResponse> responsePage = page.map(this::toSummary);
        return PagedResponse.from(responsePage);
    }

    private Patient findPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PATIENT_NOT_FOUND));
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse()
                .id(p.getId())
                .name(p.getName())
                .age(computeAge(p.getDob()))
                .gender(p.getGender())
                .dob(p.getDob() != null ? p.getDob().toLocalDate() : null)
                .mobileNumber(p.getMobileNumber())
                .email(p.getEmail())
                .address(p.getAddress())
                .emergencyContactNumber(p.getEmergencyContactNumber())
                .bloodGroup(p.getBloodGroup() != null ? PatientResponse.BloodGroupEnum.fromValue(p.getBloodGroup()) : null)
                .permanentIllness(p.getPermanentIllness())
                .createdAt(toOffsetDateTime(p.getCreatedAt()));
    }

    private PatientSummaryResponse toSummary(Patient p) {
        return new PatientSummaryResponse()
                .id(p.getId())
                .name(p.getName())
                .mobileNumber(p.getMobileNumber())
                .age(computeAge(p.getDob()))
                .gender(p.getGender());
    }

    private Integer computeAge(Date dob) {
        return dob != null ? Period.between(dob.toLocalDate(), java.time.LocalDate.now()).getYears() : null;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}