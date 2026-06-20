package com.healthcare.controller;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.ApiResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.patient.PatientResponse;
import com.healthcare.dto.patient.PatientSummaryResponse;
import com.healthcare.dto.patient.PatientUpdateRequest;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PatientResponse>> getProfile() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("PATIENT");
        PatientResponse response = patientService.getProfile(caller.userId());
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PATIENT_FETCHED, response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PatientResponse>> updateProfile(
            @RequestBody PatientUpdateRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("PATIENT");
        PatientResponse response = patientService.updateProfile(caller.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PATIENT_PROFILE_UPDATED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("ADMIN", "DOCTOR");
        PatientResponse response = patientService.getPatientById(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PATIENT_FETCHED, response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<PatientSummaryResponse>>> searchPatients(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SecurityUtils.requireRole("ADMIN");
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PatientSummaryResponse> response = patientService.searchPatients(query, pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PATIENT_FETCHED, response));
    }
}
