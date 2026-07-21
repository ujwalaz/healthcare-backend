package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.*;
import com.healthcare.event.AppointmentEvent;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.*;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND);
        }

        int dayOfWeekIndex = date.getDayOfWeek().getValue() - 1; // 0=Mon..6=Sun
        List<DoctorSchedule> sessions = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayOfWeekIndex);

        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Appointment> booked = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, Date.valueOf(date));
        Set<LocalTime> bookedTimes = booked.stream()
                .map(a -> a.getStartTime().toLocalTime())
                .collect(Collectors.toSet());

        List<SlotResponse> slots = new ArrayList<>();
        for (DoctorSchedule session : sessions) {
            LocalTime current = session.getStartTime().toLocalTime();
            LocalTime end     = session.getEndTime().toLocalTime();
            int slotMins = session.getSlotDurationMinutes();
            while (!current.plusMinutes(slotMins).isAfter(end)) {
                if (!bookedTimes.contains(current)) {
                    slots.add(new SlotResponse()
                            .startTime(current)
                            .endTime(current.plusMinutes(slotMins)));
                }
                current = current.plusMinutes(slotMins);
            }
        }

        slots.sort(Comparator.comparing(SlotResponse::getStartTime));
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
        List<DoctorSchedule> sessions = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrue(req.getDoctorId(), dayOfWeekIndex);
        if (sessions.isEmpty()) {
            throw new ConflictException(MessageCode.APPOINTMENT_SLOT_INVALID,
                    "No active schedule for this doctor on the requested day");
        }

        DoctorSchedule schedule = sessions.stream()
                .filter(s -> isValidSlot(req.getStartTime(), s))
                .findFirst()
                .orElseThrow(() -> new ConflictException(MessageCode.APPOINTMENT_SLOT_INVALID,
                        "The requested start time does not align with the doctor's schedule slots"));

        LocalTime endTime = req.getStartTime().plusMinutes(schedule.getSlotDurationMinutes());

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTime(
                req.getDoctorId(), Date.valueOf(req.getAppointmentDate()), Time.valueOf(req.getStartTime()))) {
            throw new ConflictException(MessageCode.APPOINTMENT_SLOT_UNAVAILABLE);
        }

        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(req.getDoctorId())
                .hospitalId(req.getHospitalId())
                .appointmentDate(Date.valueOf(req.getAppointmentDate()))
                .startTime(Time.valueOf(req.getStartTime()))
                .endTime(Time.valueOf(endTime))
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
        LocalDateTime accessExpiresAt = LocalDateTime.of(req.getAppointmentDate(), endTime);        DoctorPmrAccessLog accessLog = DoctorPmrAccessLog.builder()
                .doctorId(req.getDoctorId())
                .patientId(patientId)
                .appointmentId(appointment.getId())
                .accessGrantedAt(LocalDateTime.now())
                .accessExpiresAt(accessExpiresAt)
                .build();
        doctorPmrAccessLogRepository.save(accessLog);

        log.info("Appointment booked with id={} for patientId={}", appointment.getId(), patientId);

        eventPublisher.publishEvent(new AppointmentEvent(
                appointment.getId(), "BOOKED",
                patientId, patient.getName(),
                req.getDoctorId(), doctor.getName(),
                req.getHospitalId(),
                req.getAppointmentDate().format(DATE_FMT),
                req.getStartTime().format(TIME_FMT),
                caller.role()
        ));

        return buildAppointmentResponse(appointment, patient, doctor, hospital);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AppointmentSummaryResponse> getAppointments(
            String status, LocalDate date, Pageable pageable, JwtClaims caller) {

        Long patientId = "PATIENT".equals(caller.role()) ? caller.userId() : null;
        Long doctorId = "DOCTOR".equals(caller.role()) ? caller.userId() : null;
        Long hospitalId = "ADMIN".equals(caller.role()) ? caller.hospitalId() : null;

        Page<Appointment> page = appointmentRepository.findWithFilters(
                patientId, doctorId, hospitalId, status, date != null ? Date.valueOf(date) : null, pageable);

        Page<AppointmentSummaryResponse> responsePage = page.map(a -> {
            Patient patient = patientRepository.findById(a.getPatientId()).orElse(null);
            Doctor doctor = doctorRepository.findById(a.getDoctorId()).orElse(null);
            return new AppointmentSummaryResponse()
                    .id(a.getId())
                    .patientName(patient != null ? patient.getName() : "Unknown")
                    .doctorName(doctor != null ? doctor.getName() : "Unknown")
                    .appointmentDate(a.getAppointmentDate().toLocalDate())
                    .startTime(a.getStartTime().toLocalTime())
                    .endTime(a.getEndTime().toLocalTime())
                    .status(a.getStatus())
                    .isOffline(a.getIsOffline());
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

        if ("CANCELLED".equals(newStatus) || "COMPLETED".equals(newStatus)) {
            Patient patient2 = patient;
            Doctor doctor2 = doctor;
            if (patient2 == null) patient2 = patientRepository.findById(appointment.getPatientId()).orElse(null);
            if (doctor2 == null) doctor2 = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
            String pName = patient2 != null ? patient2.getName() : "Patient";
            String dName = doctor2 != null ? doctor2.getName() : "Doctor";
            eventPublisher.publishEvent(new AppointmentEvent(
                    appointment.getId(), newStatus,
                    appointment.getPatientId(), pName,
                    appointment.getDoctorId(), dName,
                    appointment.getHospitalId(),
                    appointment.getAppointmentDate().toLocalDate().format(DATE_FMT),
                    appointment.getStartTime().toLocalTime().format(TIME_FMT),
                    caller.role()
            ));
        }

        return buildAppointmentResponse(appointment, patient, doctor, hospital);
    }

    private boolean isValidSlot(LocalTime requestedTime, DoctorSchedule schedule) {
        LocalTime current = schedule.getStartTime().toLocalTime();
        LocalTime end     = schedule.getEndTime().toLocalTime();
        int slotMins = schedule.getSlotDurationMinutes();
        while (current.plusMinutes(slotMins).compareTo(end) <= 0) {
            if (current.equals(requestedTime)) return true;
            current = current.plusMinutes(slotMins);
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
        return new AppointmentResponse()
                .id(a.getId())
                .patientId(a.getPatientId())
                .patientName(patient != null ? patient.getName() : "Unknown")
                .doctorId(a.getDoctorId())
                .doctorName(doctor != null ? doctor.getName() : "Unknown")
                .hospitalId(a.getHospitalId())
                .hospitalName(hospital != null ? hospital.getName() : "Unknown")
                .appointmentDate(a.getAppointmentDate().toLocalDate())
                .startTime(a.getStartTime().toLocalTime())
                .endTime(a.getEndTime().toLocalTime())
                .status(a.getStatus())
                .isOffline(a.getIsOffline())
                .notes(a.getNotes())
                .bookedByRole(a.getBookedByRole())
                .createdAt(toOffsetDateTime(a.getCreatedAt()));
    }


    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}