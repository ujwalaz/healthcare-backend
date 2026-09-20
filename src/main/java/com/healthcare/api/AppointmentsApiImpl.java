package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.AppointmentRequest;
import com.healthcare.dto.AppointmentResponse;
import com.healthcare.dto.AppointmentSummaryPagedResponse;
import com.healthcare.dto.SlotResponse;
import com.healthcare.dto.StatusUpdateRequest;
import com.healthcare.dto.TodaySlotsSummaryResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppointmentsApiImpl implements AppointmentsApi {

    private final AppointmentService appointmentService;

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_SLOTS_FETCHED)
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(Long doctorId, LocalDate date) {
        List<SlotResponse> slots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_SLOTS_SUMMARY_FETCHED)
    public ResponseEntity<TodaySlotsSummaryResponse> getTodaySlotsSummary() {
        return ResponseEntity.ok(appointmentService.getTodaySlotsSummary());
    }

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_BOOKED)
    public ResponseEntity<AppointmentResponse> bookAppointment(AppointmentRequest appointmentRequest) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.bookAppointment(appointmentRequest, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_FETCHED)
    public ResponseEntity<AppointmentSummaryPagedResponse> getAppointments(String status, LocalDate date, Integer page, Integer size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PaginationUtils.of(page, size);
        PagedResponse<com.healthcare.dto.AppointmentSummaryResponse> paged =
                appointmentService.getAppointments(status, date, pageable, caller);
        return ResponseEntity.ok(toPaged(paged));
    }

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_FETCHED)
    public ResponseEntity<AppointmentResponse> getAppointmentById(Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.getAppointmentById(id, caller);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.APPOINTMENT_STATUS_UPDATED)
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(Long id, StatusUpdateRequest statusUpdateRequest) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        AppointmentResponse response = appointmentService.updateStatus(id, statusUpdateRequest, caller);
        return ResponseEntity.ok(response);
    }

    private AppointmentSummaryPagedResponse toPaged(PagedResponse<com.healthcare.dto.AppointmentSummaryResponse> paged) {
        return new AppointmentSummaryPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
    }
}
