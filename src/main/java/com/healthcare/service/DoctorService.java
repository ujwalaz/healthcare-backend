package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.DoctorSchedule;
import com.healthcare.entity.Hospital;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.DoctorScheduleRepository;
import com.healthcare.repository.HospitalRepository;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final HospitalRepository hospitalRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public PagedResponse<DoctorSummaryResponse> getDoctors(Long hospitalId, Pageable pageable) {
        Page<Doctor> page;
        if (hospitalId != null) {
            page = doctorRepository.findByHospitalId(hospitalId, pageable);
        } else {
            page = doctorRepository.findAll(pageable);
        }
        Page<DoctorSummaryResponse> responsePage = page.map(this::toSummary);
        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND));

        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.HOSPITAL_NOT_FOUND));

        return new DoctorResponse()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .education(doctor.getEducation())
                .phone(doctor.getPhone())
                .email(doctor.getEmail())
                .hospitalId(doctor.getHospitalId())
                .hospitalName(hospital.getName())
                .isActive(doctor.getIsActive());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedule(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND);
        }
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndIsActiveTrue(doctorId);
        return schedules.stream().map(this::toScheduleResponse).toList();
    }

    @Transactional
    public List<ScheduleResponse> saveSchedule(Long doctorId, List<ScheduleRequest> requests, JwtClaims caller) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND));

        if ("DOCTOR".equals(caller.role())) {
            if (!caller.userId().equals(doctorId)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Doctors can only manage their own schedule");
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (!caller.hospitalId().equals(doctor.getHospitalId())) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Admin can only manage schedules within their hospital");
            }
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        List<DoctorSchedule> savedSchedules = requests.stream().map(req -> {
            String sessionTypeStr = req.getSessionType().getValue();
            Optional<DoctorSchedule> existing = scheduleRepository
                    .findByDoctorIdAndDayOfWeekAndSessionType(doctorId, req.getDayOfWeek(), sessionTypeStr);

            DoctorSchedule schedule;
            if (existing.isPresent()) {
                schedule = existing.get();
                schedule.setStartTime(Time.valueOf(req.getStartTime()));
                schedule.setEndTime(Time.valueOf(req.getEndTime()));
                schedule.setSlotDurationMinutes(req.getSlotDurationMinutes());
                schedule.setSessionType(sessionTypeStr);
                schedule.setIsActive(true);
            } else {
                schedule = DoctorSchedule.builder()
                        .doctorId(doctorId)
                        .dayOfWeek(req.getDayOfWeek())
                        .sessionType(sessionTypeStr)
                        .startTime(Time.valueOf(req.getStartTime()))
                        .endTime(Time.valueOf(req.getEndTime()))
                        .slotDurationMinutes(req.getSlotDurationMinutes())
                        .isActive(true)
                        .build();
            }
            return scheduleRepository.save(schedule);
        }).toList();

        // Deactivate sessions whose (dayOfWeek, sessionType) is no longer in the request
        Set<String> requestedKeys = requests.stream()
                .map(r -> r.getDayOfWeek() + "_" + r.getSessionType().getValue())
                .collect(Collectors.toSet());
        scheduleRepository.findByDoctorId(doctorId).stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> !requestedKeys.contains(s.getDayOfWeek() + "_" + s.getSessionType()))
                .forEach(s -> {
                    s.setIsActive(false);
                    scheduleRepository.save(s);
                });

        log.info("Saved {} schedule entries for doctorId={}", savedSchedules.size(), doctorId);
        return savedSchedules.stream().map(this::toScheduleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> getCalendar(Long doctorId, LocalDate from, LocalDate to, JwtClaims caller) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND));

        if ("DOCTOR".equals(caller.role())) {
            if (!caller.userId().equals(doctorId)) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Doctors can only view their own calendar");
            }
        } else if ("ADMIN".equals(caller.role())) {
            if (caller.hospitalId() == null || !caller.hospitalId().equals(doctor.getHospitalId())) {
                throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                        "Admin can only view calendars within their hospital");
            }
        } else {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED);
        }

        // Cap range at 30 days for safety
        if (to.isAfter(from.plusDays(30))) {
            to = from.plusDays(30);
        }

        List<CalendarDayResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            int dayIndex = d.getDayOfWeek().getValue() - 1; // 0=Mon..6=Sun
            List<DoctorSchedule> sessions = scheduleRepository
                    .findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayIndex);

            if (sessions.isEmpty()) {
                result.add(new CalendarDayResponse()
                        .date(d).status(CalendarDayResponse.StatusEnum.NO_SCHEDULE)
                            .totalSlots(0).bookedSlots(0).availableSlots(0)
                            .morningTotalSlots(0).morningAvailableSlots(0)
                            .eveningTotalSlots(0).eveningAvailableSlots(0));
                    continue;
                }

                Time noon = Time.valueOf(LocalTime.NOON);

                int morningTotalSlots = countSlots(sessions, "MORNING");
                int eveningTotalSlots = countSlots(sessions, "EVENING");
                int totalSlots = morningTotalSlots + eveningTotalSlots;

                int morningBookedSlots = appointmentRepository
                        .countByDoctorIdAndAppointmentDateAndStartTimeLessThanAndStatusNot(
                                doctorId, Date.valueOf(d), noon, "CANCELLED");
                int bookedSlots = appointmentRepository
                        .countByDoctorIdAndAppointmentDateAndStatusNot(doctorId, Date.valueOf(d), "CANCELLED");
                int eveningBookedSlots = bookedSlots - morningBookedSlots;

                int morningAvailableSlots = Math.max(0, morningTotalSlots - morningBookedSlots);
                int eveningAvailableSlots = Math.max(0, eveningTotalSlots - eveningBookedSlots);
                int availableSlots = morningAvailableSlots + eveningAvailableSlots;
            CalendarDayResponse.StatusEnum status = availableSlots > 0
                    ? CalendarDayResponse.StatusEnum.AVAILABLE
                    : CalendarDayResponse.StatusEnum.FULL;

            result.add(new CalendarDayResponse()
                    .date(d).status(status)
                    .totalSlots(totalSlots)
                    .bookedSlots(bookedSlots)
                    .availableSlots(availableSlots)
                    .morningTotalSlots(morningTotalSlots)
                    .morningAvailableSlots(morningAvailableSlots)
                    .eveningTotalSlots(eveningTotalSlots)
                    .eveningAvailableSlots(eveningAvailableSlots));
        }

        return result;
    }

    private int countSlots(List<DoctorSchedule> sessions, String sessionType) {
        return sessions.stream()
                .filter(s -> sessionType.equals(s.getSessionType()))
                .mapToInt(s -> {
                    LocalTime cur = s.getStartTime().toLocalTime();
                    LocalTime end = s.getEndTime().toLocalTime();
                    int slotMins = s.getSlotDurationMinutes();
                    int count = 0;
                    while (!cur.plusMinutes(slotMins).isAfter(end)) {
                        count++;
                        cur = cur.plusMinutes(slotMins);
                    }
                    return count;
                }).sum();
    }

    private DoctorSummaryResponse toSummary(Doctor d) {
        return new DoctorSummaryResponse()
                .id(d.getId())
                .name(d.getName())
                .specialization(d.getSpecialization())
                .education(d.getEducation())
                .isActive(d.getIsActive());
    }

    private ScheduleResponse toScheduleResponse(DoctorSchedule s) {
        return new ScheduleResponse()
                .id(s.getId())
                .dayOfWeek(s.getDayOfWeek())
                .dayLabel(computeDayLabel(s.getDayOfWeek()))
                .sessionType(s.getSessionType() != null
                        ? ScheduleResponse.SessionTypeEnum.fromValue(s.getSessionType()) : null)
                .startTime(s.getStartTime().toLocalTime())
                .endTime(s.getEndTime().toLocalTime())
                .slotDurationMinutes(s.getSlotDurationMinutes())
                .isActive(s.getIsActive());
    }


    private String computeDayLabel(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 0 -> "Monday";
            case 1 -> "Tuesday";
            case 2 -> "Wednesday";
            case 3 -> "Thursday";
            case 4 -> "Friday";
            case 5 -> "Saturday";
            case 6 -> "Sunday";
            default -> "Unknown";
        };
    }
}