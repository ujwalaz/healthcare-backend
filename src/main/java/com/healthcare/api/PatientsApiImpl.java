package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.PatientResponse;
import com.healthcare.dto.PatientSummaryPagedResponse;
import com.healthcare.dto.PatientSummaryResponse;
import com.healthcare.dto.PatientUpdateRequest;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PatientsApiImpl implements PatientsApi {

    private final PatientService patientService;

    @Override
    @ApiMessage(MessageCode.PATIENT_FETCHED)
    public ResponseEntity<PatientResponse> getPatientProfile() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("PATIENT");
        PatientResponse response = patientService.getProfile(caller.userId());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.PATIENT_PROFILE_UPDATED)
    public ResponseEntity<PatientResponse> updatePatientProfile(PatientUpdateRequest patientUpdateRequest) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("PATIENT");
        PatientResponse response = patientService.updateProfile(caller.userId(), patientUpdateRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.PATIENT_FETCHED)
    public ResponseEntity<PatientResponse> getPatientById(Long id) {
        SecurityUtils.requireRole("ADMIN", "DOCTOR");
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.PATIENT_FETCHED)
    public ResponseEntity<PatientSummaryPagedResponse> searchPatients(String query, Integer page, Integer size) {
        SecurityUtils.requireRole("ADMIN");
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PatientSummaryResponse> paged = patientService.searchPatients(query, pageable);
        PatientSummaryPagedResponse response = new PatientSummaryPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }
}
