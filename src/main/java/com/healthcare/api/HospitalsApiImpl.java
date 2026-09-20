package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.HospitalDetailResponse;
import com.healthcare.dto.HospitalPagedResponse;
import com.healthcare.dto.HospitalResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HospitalsApiImpl implements HospitalsApi {

    private final HospitalService hospitalService;

    @Override
    @ApiMessage(MessageCode.HOSPITAL_FETCHED)
    public ResponseEntity<HospitalPagedResponse> getAllHospitals(Integer page, Integer size) {
        Pageable pageable = PaginationUtils.of(page, size);
        PagedResponse<HospitalResponse> paged = hospitalService.getAllHospitals(pageable);
        HospitalPagedResponse response = new HospitalPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.HOSPITAL_FETCHED)
    public ResponseEntity<HospitalDetailResponse> getHospitalById(Long id) {
        HospitalDetailResponse response = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(response);
    }
}
