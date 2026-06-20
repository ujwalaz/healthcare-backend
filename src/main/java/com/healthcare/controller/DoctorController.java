package com.healthcare.controller;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.ApiResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.dto.doctor.DoctorSummaryResponse;
import com.healthcare.dto.doctor.ScheduleRequest;
import com.healthcare.dto.doctor.ScheduleResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DoctorSummaryResponse>>> getDoctors(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<DoctorSummaryResponse> response = doctorService.getDoctors(hospitalId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCTOR_FETCHED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCTOR_FETCHED, response));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedule(@PathVariable Long id) {
        List<ScheduleResponse> response = doctorService.getSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCTOR_SCHEDULE_FETCHED, response));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> saveSchedule(
            @PathVariable Long id,
            @Valid @RequestBody List<@Valid ScheduleRequest> requests) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        List<ScheduleResponse> response = doctorService.saveSchedule(id, requests, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.DOCTOR_SCHEDULE_SAVED, response));
    }
}
