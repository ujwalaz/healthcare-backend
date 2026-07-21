package com.healthcare.controller;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.*;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SlotResponse> slots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.APPOINTMENT_SLOTS_FETCHED, slots));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.bookAppointment(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageCode.APPOINTMENT_BOOKED, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentSummaryResponse>>> getAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<AppointmentSummaryResponse> response =
                appointmentService.getAppointments(status, date, pageable, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.APPOINTMENT_FETCHED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.getAppointmentById(id, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.APPOINTMENT_FETCHED, response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.updateStatus(id, request, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.APPOINTMENT_STATUS_UPDATED, response));
    }
}
