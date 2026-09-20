package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.PmrEntryPagedResponse;
import com.healthcare.dto.PmrEntryRequest;
import com.healthcare.dto.PmrEntryResponse;
import com.healthcare.dto.PmrResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.PmrService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PmrApiImpl implements PmrApi {

    private final PmrService pmrService;

    @Override
    @ApiMessage(MessageCode.PMR_FETCHED)
    public ResponseEntity<PmrResponse> getPmr(Long patientId, Long hospitalId) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        PmrResponse response = pmrService.getPmr(patientId, hospitalId, caller);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.PMR_ENTRIES_FETCHED)
    public ResponseEntity<PmrEntryPagedResponse> getPmrEntries(Long patientId, Long hospitalId, Integer page, Integer size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PaginationUtils.of(page, size);
        PagedResponse<PmrEntryResponse> paged = pmrService.getPmrEntries(patientId, hospitalId, pageable, caller);
        PmrEntryPagedResponse response = new PmrEntryPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.PMR_ENTRY_CREATED)
    public ResponseEntity<PmrEntryResponse> addPmrEntry(Long patientId, PmrEntryRequest pmrEntryRequest, Long hospitalId) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        PmrEntryResponse response = pmrService.addPmrEntry(patientId, hospitalId, pmrEntryRequest, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
