package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.PatientResponse;
import com.healthcare.dto.PatientSummaryResponse;
import com.healthcare.dto.PatientUpdateRequest;
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
        if (request.getAge() != null) {
            patient.setAge(request.getAge());
        }
        if (request.getDob() != null) {
            patient.setDob(request.getDob());
        }
        if (request.getEmail() != null) {
            patient.setEmail(request.getEmail());
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
        return PatientResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .age(p.getAge())
                .gender(p.getGender())
                .dob(p.getDob())
                .mobileNumber(p.getMobileNumber())
                .email(p.getEmail())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PatientSummaryResponse toSummary(Patient p) {
        return PatientSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .mobileNumber(p.getMobileNumber())
                .age(p.getAge())
                .gender(p.getGender())
                .build();
    }
}
