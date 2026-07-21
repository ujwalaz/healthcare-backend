package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.DoctorSummaryResponse;
import com.healthcare.dto.HospitalDetailResponse;
import com.healthcare.dto.HospitalResponse;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Hospital;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public PagedResponse<HospitalResponse> getAllHospitals(Pageable pageable) {
        Page<Hospital> page = hospitalRepository.findAll(pageable);
        Page<HospitalResponse> responsePage = page.map(this::toResponse);
        log.info("Fetched {} hospitals", page.getTotalElements());
        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public HospitalDetailResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.HOSPITAL_NOT_FOUND));

        List<Doctor> doctors = doctorRepository.findByHospitalId(id);
        List<DoctorSummaryResponse> doctorSummaries = doctors.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .map(this::toDoctorSummary)
                .toList();

        return HospitalDetailResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .address(hospital.getAddress())
                .city(hospital.getCity())
                .phone(hospital.getPhone())
                .email(hospital.getEmail())
                .doctors(doctorSummaries)
                .build();
    }

    private HospitalResponse toResponse(Hospital h) {
        return HospitalResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .city(h.getCity())
                .phone(h.getPhone())
                .email(h.getEmail())
                .build();
    }

    private DoctorSummaryResponse toDoctorSummary(Doctor d) {
        return DoctorSummaryResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .specialization(d.getSpecialization())
                .education(d.getEducation())
                .isActive(d.getIsActive())
                .build();
    }
}
