package com.healthcare.controller;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.ApiResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.HospitalDetailResponse;
import com.healthcare.dto.HospitalResponse;
import com.healthcare.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<HospitalResponse>>> getAllHospitals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<HospitalResponse> response = hospitalService.getAllHospitals(pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.HOSPITAL_FETCHED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalDetailResponse>> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse response = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.HOSPITAL_FETCHED, response));
    }
}
