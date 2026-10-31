package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.CalendarDayResponse;
import com.healthcare.dto.DoctorResponse;
import com.healthcare.dto.DoctorSummaryPagedResponse;
import com.healthcare.dto.DoctorSummaryResponse;
import com.healthcare.dto.DoctorProfileUpdateRequest;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.ScheduleRequest;
import com.healthcare.dto.ScheduleResponse;
import com.healthcare.dto.DocumentResponse;
import com.healthcare.dto.DownloadUrlResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.DoctorService;
import com.healthcare.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorsApiImpl implements DoctorsApi {

    private final DoctorService doctorService;
    private final DocumentService documentService;

    @Override
    @ApiMessage(MessageCode.DOCTOR_FETCHED)
    public ResponseEntity<DoctorSummaryPagedResponse> getDoctors(Long hospitalId, Integer page, Integer size) {
        Pageable pageable = PaginationUtils.of(page, size);
        PagedResponse<DoctorSummaryResponse> paged = doctorService.getDoctors(hospitalId, pageable);
        DoctorSummaryPagedResponse response = new DoctorSummaryPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_FETCHED)
    public ResponseEntity<DoctorResponse> getDoctorById(Long id) {
        DoctorResponse response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_FETCHED)
    public ResponseEntity<DoctorResponse> getDoctorProfile() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("DOCTOR");
        return ResponseEntity.ok(doctorService.getDoctorById(caller.userId()));
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_PROFILE_UPDATED)
    public ResponseEntity<DoctorResponse> updateDoctorProfile(DoctorProfileUpdateRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("DOCTOR");
        return ResponseEntity.ok(doctorService.updateProfile(caller.userId(), request));
    }

    @Override
    @ApiMessage(MessageCode.DOCUMENT_UPLOADED)
    public ResponseEntity<DocumentResponse> uploadDoctorProfilePicture(MultipartFile file) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("DOCTOR");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(documentService.uploadDoctorProfilePicture(file, caller));
    }

    @Override
    @ApiMessage(MessageCode.DOCUMENT_DOWNLOAD_URL_READY)
    public ResponseEntity<DownloadUrlResponse> getDoctorProfilePictureDownloadUrl() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("DOCTOR");
        return ResponseEntity.ok(documentService.getDoctorProfilePictureDownloadUrl(caller.userId(), caller));
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_SCHEDULE_FETCHED)
    public ResponseEntity<List<ScheduleResponse>> getDoctorSchedule(Long id) {
        List<ScheduleResponse> response = doctorService.getSchedule(id);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_SCHEDULE_SAVED)
    public ResponseEntity<List<ScheduleResponse>> saveDoctorSchedule(Long id, List<ScheduleRequest> scheduleRequest) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        List<ScheduleResponse> response = doctorService.saveSchedule(id, scheduleRequest, caller);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.DOCTOR_CALENDAR_FETCHED)
    public ResponseEntity<List<CalendarDayResponse>> getDoctorCalendar(Long id, LocalDate from, LocalDate to) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        List<CalendarDayResponse> response = doctorService.getCalendar(id, from, to, caller);
        return ResponseEntity.ok(response);
    }
}