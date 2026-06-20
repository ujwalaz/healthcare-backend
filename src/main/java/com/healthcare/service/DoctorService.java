package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.doctor.DoctorResponse;
import com.healthcare.dto.doctor.DoctorSummaryResponse;
import com.healthcare.dto.doctor.ScheduleRequest;
import com.healthcare.dto.doctor.ScheduleResponse;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.DoctorSchedule;
import com.healthcare.entity.Hospital;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final HospitalRepository hospitalRepository;

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

        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .education(doctor.getEducation())
                .phone(doctor.getPhone())
                .email(doctor.getEmail())
                .hospitalId(doctor.getHospitalId())
                .hospitalName(hospital.getName())
                .isActive(doctor.getIsActive())
                .build();
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
            Optional<DoctorSchedule> existing = scheduleRepository
                    .findByDoctorIdAndDayOfWeek(doctorId, req.getDayOfWeek());

            DoctorSchedule schedule;
            if (existing.isPresent()) {
                schedule = existing.get();
                schedule.setStartTime(req.getStartTime());
                schedule.setEndTime(req.getEndTime());
                schedule.setSlotDurationMinutes(req.getSlotDurationMinutes());
                schedule.setIsActive(true);
            } else {
                schedule = DoctorSchedule.builder()
                        .doctorId(doctorId)
                        .dayOfWeek(req.getDayOfWeek())
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .slotDurationMinutes(req.getSlotDurationMinutes())
                        .isActive(true)
                        .build();
            }
            return scheduleRepository.save(schedule);
        }).toList();

        log.info("Saved {} schedule entries for doctorId={}", savedSchedules.size(), doctorId);
        return savedSchedules.stream().map(this::toScheduleResponse).toList();
    }

    private DoctorSummaryResponse toSummary(Doctor d) {
        return DoctorSummaryResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .specialization(d.getSpecialization())
                .education(d.getEducation())
                .isActive(d.getIsActive())
                .build();
    }

    private ScheduleResponse toScheduleResponse(DoctorSchedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .dayOfWeek(s.getDayOfWeek())
                .dayLabel(ScheduleResponse.computeDayLabel(s.getDayOfWeek()))
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .slotDurationMinutes(s.getSlotDurationMinutes())
                .isActive(s.getIsActive())
                .build();
    }
}
