package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.auth.*;
import com.healthcare.entity.Patient;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.AdminUser;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AdminUserRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (patientRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS);
        }

        Patient patient = Patient.builder()
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .dob(request.getDob())
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .createdAt(LocalDateTime.now())
                .build();

        patient = patientRepository.save(patient);
        log.info("Registered new patient with id={}", patient.getId());

        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null);
        return AuthResponse.builder()
                .token(token)
                .id(patient.getId())
                .name(patient.getName())
                .role("PATIENT")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse loginPatient(PatientLoginRequest request) {
        Patient patient = patientRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.PATIENT_NOT_FOUND));

        log.info("Patient login successful for id={}", patient.getId());
        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null);
        return AuthResponse.builder()
                .token(token)
                .id(patient.getId())
                .name(patient.getName())
                .role("PATIENT")
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse loginDoctor(DoctorLoginRequest request) {
        Doctor doctor = doctorRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.DOCTOR_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), doctor.getPasswordHash())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_ACCOUNT_INACTIVE);
        }

        log.info("Doctor login successful for id={}", doctor.getId());
        String token = jwtUtil.generateToken(doctor.getId(), "DOCTOR", doctor.getHospitalId());
        return AuthResponse.builder()
                .token(token)
                .id(doctor.getId())
                .name(doctor.getName())
                .role("DOCTOR")
                .hospitalId(doctor.getHospitalId())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse loginAdmin(AdminLoginRequest request) {
        AdminUser admin = adminUserRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_ACCOUNT_INACTIVE);
        }

        log.info("Admin login successful for id={}", admin.getId());
        String token = jwtUtil.generateToken(admin.getId(), "ADMIN", admin.getHospitalId());
        return AuthResponse.builder()
                .token(token)
                .id(admin.getId())
                .name(admin.getName())
                .role("ADMIN")
                .hospitalId(admin.getHospitalId())
                .build();
    }
}
