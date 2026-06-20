package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.appointment.*;
import com.healthcare.entity.*;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.*;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PatientHospitalLinkRepository patientHospitalLinkRepository;
    private final PatientMedicalRecordRepository patientMedicalRecordRepository;
    private final DoctorPmrAccessLogRepository doctorPmrAccessLogRepository;

    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND);
        }

        int dayOfWeekIndex = date.getDayOfWeek().getValue() - 1; // 0=Mon..6=Sun
        DoctorSchedule schedule = scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeekIndex)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .orElse(null);

        if (schedule == null) {
            return List.of();
        }

        List<Appointment> booked = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date);
        Set<LocalTime> bookedTimes = booked.stream()
                .map(Appointment::getStartTime)
                .collect(Collectors.toSet());

        List<SlotResponse> slots = new ArrayList<>();
        LocalTime current = schedule.getStartTime();
        while (current.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(schedule.getEndTime()) <= 0) {
            if (!bookedTimes.contains(current)) {
                slots.add(new SlotResponse(current, current.plusMinutes(schedule.getSlotDurationMinutes())));
            }
            current = current.plusMinutes(schedule.getSlotDurationMinutes());
        }

        log.info("Found {} available slots for doctorId={} on date={}", slots.size(), doctorId, date);
        return slots;
    }

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest req, JwtClaims caller) {
        Long patientId;
        if ("PATIENT".equals(caller.role())) {
            patientId = caller.userId();
        } else if ("ADMIN".equals(caller.role())) {
            if (req.getPatientId() == null) {
                throw new AppDeniedException(MessageCode.VALIDATION_FAILED, "Patient ID is required");
            }
            if (!caller.hospitalId().equals(req.getHospitalId())) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Admin can only book appointments in their hospital");
            }
            patientId = req.getPatientId();
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PATIENT_NOT_FOUND));
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND));
        Hospital hospital = hospitalRepository.findById(req.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.HOSPITAL_NOT_FOUND));

        int dayOfWeekIndex = req.getAppointmentDate().getDayOfWeek().getValue() - 1;
        DoctorSchedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeek(req.getDoctorId(), dayOfWeekIndex)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .orElseThrow(() -> new ConflictException(MessageCode.APPOINTMENT_SLOT_INVALID,
                        "No active schedule for this doctor on the requested day"));

        LocalTime endTime = req.getStartTime().plusMinutes(schedule.getSlotDurationMinutes());
        boolean validSlot = isValidSlot(req.getStartTime(), schedule);
        if (!validSlot) {
            throw new ConflictException(MessageCode.APPOINTMENT_SLOT_INVALID,
                    "The requested start time does not align with the doctor's schedule slots");
        }

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTime(
                req.getDoctorId(), req.getAppointmentDate(), req.getStartTime())) {
            throw new ConflictException(MessageCode.APPOINTMENT_SLOT_UNAVAILABLE);
        }

        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(req.getDoctorId())
                .hospitalId(req.getHospitalId())
                .appointmentDate(req.getAppointmentDate())
                .startTime(req.getStartTime())
                .endTime(endTime)
                .bookedByRole(caller.role())
                .bookedById(caller.userId())
                .isOffline(req.getIsOffline() != null ? req.getIsOffline() : false)
                .status("SCHEDULED")
                .notes(req.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            appointment = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(MessageCode.APPOINTMENT_SLOT_UNAVAILABLE);
        }

        // Upsert patient-hospital link
        PatientHospitalLinkId linkId = new PatientHospitalLinkId(patientId, req.getHospitalId());
        if (!patientHospitalLinkRepository.existsById(linkId)) {
            patientHospitalLinkRepository.save(PatientHospitalLink.builder()
                    .id(linkId)
                    .linkedAt(LocalDateTime.now())
                    .build());
        }

        // Upsert patient medical record
        patientMedicalRecordRepository.findByPatientIdAndHospitalId(patientId, req.getHospitalId())
                .orElseGet(() -> patientMedicalRecordRepository.save(PatientMedicalRecord.builder()
                        .patientId(patientId)
                        .hospitalId(req.getHospitalId())
                        .createdAt(LocalDateTime.now())
                        .build()));

        // Save doctor PMR access log
        LocalDateTime accessExpiresAt = LocalDateTime.of(req.getAppointmentDate(), endTime);
        DoctorPmrAccessLog accessLog = DoctorPmrAccessLog.builder()
                .doctorId(req.getDoctorId())
                .patientId(patientId)
                .appointmentId(appointment.getId())
                .accessGrantedAt(LocalDateTime.now())
                .accessExpiresAt(accessExpiresAt)
                .build();
        doctorPmrAccessLogRepository.save(accessLog);

        log.info("Appointment booked with id={} for patientId={}", appointment.getId(), patientId);
        return buildAppointmentResponse(appointment, patient, doctor, hospital);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AppointmentSummaryResponse> getAppointments(
            String status, LocalDate date, Pageable pageable, JwtClaims caller) {

        Long patientId = "PATIENT".equals(caller.role()) ? caller.userId() : null;
        Long doctorId = "DOCTOR".equals(caller.role()) ? caller.userId() : null;
        Long hospitalId = "ADMIN".equals(caller.role()) ? caller.hospitalId() : null;

        Page<Appointment> page = appointmentRepository.findWithFilters(
                patientId, doctorId, hospitalId, status, date, pageable);

        Page<AppointmentSummaryResponse> responsePage = page.map(a -> {
            Patient patient = patientRepository.findById(a.getPatientId()).orElse(null);
            Doctor doctor = doctorRepository.findById(a.getDoctorId()).orElse(null);
            return AppointmentSummaryResponse.builder()
                    .id(a.getId())
                    .patientName(patient != null ? patient.getName() : "Unknown")
                    .doctorName(doctor != null ? doctor.getName() : "Unknown")
                    .appointmentDate(a.getAppointmentDate())
                    .startTime(a.getStartTime())
                    .endTime(a.getEndTime())
                    .status(a.getStatus())
                    .isOffline(a.getIsOffline())
                    .build();
        });

        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id, JwtClaims caller) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.APPOINTMENT_NOT_FOUND));

        validateAppointmentAccess(appointment, caller);

        Patient patient = patientRepository.findById(appointment.getPatientId())
                .orElse(null);
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId())
                .orElse(null);
        Hospital hospital = hospitalRepository.findById(appointment.getHospitalId())
                .orElse(null);

        return buildAppointmentResponse(appointment, patient, doctor, hospital);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, StatusUpdateRequest request, JwtClaims caller) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.APPOINTMENT_NOT_FOUND));

        String newStatus = request.getStatus().toUpperCase();

        if ("PATIENT".equals(caller.role())) {
            if (!appointment.getPatientId().equals(caller.userId())) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
            }
            if (!"CANCELLED".equals(newStatus)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Patients can only cancel appointments");
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (!caller.hospitalId().equals(appointment.getHospitalId())) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
            }
            if (!newStatus.equals("CANCELLED") && !newStatus.equals("COMPLETED")) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Admin can only cancel or complete appointments");
            }
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        if (!"SCHEDULED".equals(appointment.getStatus())) {
            throw new ConflictException(MessageCode.APPOINTMENT_TRANSITION_INVALID,
                    "Only SCHEDULED appointments can be updated. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(newStatus);
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }
        appointment = appointmentRepository.save(appointment);

        Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        Hospital hospital = hospitalRepository.findById(appointment.getHospitalId()).orElse(null);

        log.info("Appointment id={} status updated to {}", id, newStatus);
        return buildAppointmentResponse(appointment, patient, doctor, hospital);
    }

    private boolean isValidSlot(LocalTime requestedTime, DoctorSchedule schedule) {
        LocalTime current = schedule.getStartTime();
        while (current.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(schedule.getEndTime()) <= 0) {
            if (current.equals(requestedTime)) return true;
            current = current.plusMinutes(schedule.getSlotDurationMinutes());
        }
        return false;
    }

    private void validateAppointmentAccess(Appointment appointment, JwtClaims caller) {
        switch (caller.role()) {
            case "PATIENT" -> {
                if (!appointment.getPatientId().equals(caller.userId())) {
                    throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
                }
            }
            case "DOCTOR" -> {
                if (!appointment.getDoctorId().equals(caller.userId())) {
                    throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
                }
            }
            case "ADMIN" -> {
                if (!appointment.getHospitalId().equals(caller.hospitalId())) {
                    throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
                }
            }
        }
    }

    private AppointmentResponse buildAppointmentResponse(Appointment a, Patient patient, Doctor doctor, Hospital hospital) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(patient != null ? patient.getName() : "Unknown")
                .doctorId(a.getDoctorId())
                .doctorName(doctor != null ? doctor.getName() : "Unknown")
                .hospitalId(a.getHospitalId())
                .hospitalName(hospital != null ? hospital.getName() : "Unknown")
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .isOffline(a.getIsOffline())
                .notes(a.getNotes())
                .bookedByRole(a.getBookedByRole())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
