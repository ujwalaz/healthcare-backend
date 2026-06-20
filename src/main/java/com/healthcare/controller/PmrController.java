package com.healthcare.controller;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.ApiResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.pmr.PmrEntryRequest;
import com.healthcare.dto.pmr.PmrEntryResponse;
import com.healthcare.dto.pmr.PmrResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.PmrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pmr")
@RequiredArgsConstructor
public class PmrController {

    private final PmrService pmrService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<PmrResponse>> getPmr(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long hospitalId) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        PmrResponse response = pmrService.getPmr(patientId, hospitalId, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PMR_FETCHED, response));
    }

    @GetMapping("/patient/{patientId}/entries")
    public ResponseEntity<ApiResponse<PagedResponse<PmrEntryResponse>>> getPmrEntries(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<PmrEntryResponse> response = pmrService.getPmrEntries(patientId, hospitalId, pageable, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.PMR_ENTRIES_FETCHED, response));
    }

    @PostMapping("/patient/{patientId}/entry")
    public ResponseEntity<ApiResponse<PmrEntryResponse>> addPmrEntry(
            @PathVariable Long patientId,
            @RequestParam(required = false) Long hospitalId,
            @Valid @RequestBody PmrEntryRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        PmrEntryResponse response = pmrService.addPmrEntry(patientId, hospitalId, request, caller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageCode.PMR_ENTRY_CREATED, response));
    }
}
