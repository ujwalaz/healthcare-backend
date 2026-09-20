package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Patient;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.AdminUser;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AdminUserRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.security.JwtUtil;
import com.healthcare.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
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
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (patientRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS);
        }

        Patient patient = Patient.builder()
                .name(request.getName())
                .gender(request.getGender())
                .dob(Date.valueOf(request.getDob()))
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .emergencyContactNumber(request.getEmergencyContactNumber())
                .bloodGroup(request.getBloodGroup() != null ? request.getBloodGroup().getValue() : null)
                .permanentIllness(request.getPermanentIllness())
                .createdAt(LocalDateTime.now())
                .build();

        patient = patientRepository.save(patient);
        log.info("Registered new patient with id={}", patient.getId());

        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null);
        return new AuthResponse()
                .token(token)
                .id(patient.getId())
                .name(patient.getName())
                .role(AuthResponse.RoleEnum.PATIENT);
    }

    /**
     * Single entry point for PATIENT, DOCTOR, and ADMIN login. The role query
     * parameter (case-insensitive) decides which lookup/validation path runs.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(String role, LoginRequest request) {
        if (role == null) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_ROLE);
        }
        return switch (role.toUpperCase()) {
            case "PATIENT" -> loginPatient(request);
            case "DOCTOR" -> loginDoctor(request);
            case "ADMIN" -> loginAdmin(request);
            default -> throw new BadRequestException(MessageCode.AUTH_INVALID_ROLE);
        };
    }

    private AuthResponse loginPatient(LoginRequest request) {
        Patient patient = patientRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), patient.getPasswordHash())) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        log.info("Patient login successful for id={}", patient.getId());
        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null);
        return new AuthResponse()
                .token(token)
                .id(patient.getId())
                .name(patient.getName())
                .role(AuthResponse.RoleEnum.PATIENT);
    }

    private AuthResponse loginDoctor(LoginRequest request) {
        Doctor doctor = doctorRepository.findByPhone(request.getMobileNumber())
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), doctor.getPasswordHash())) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_ACCOUNT_INACTIVE);
        }

        log.info("Doctor login successful for id={}", doctor.getId());
        String token = jwtUtil.generateToken(doctor.getId(), "DOCTOR", doctor.getHospitalId());
        return new AuthResponse()
                .token(token)
                .id(doctor.getId())
                .name(doctor.getName())
                .role(AuthResponse.RoleEnum.DOCTOR)
                .hospitalId(doctor.getHospitalId());
    }

    private AuthResponse loginAdmin(LoginRequest request) {
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
        return new AuthResponse()
                .token(token)
                .id(admin.getId())
                .name(admin.getName())
                .role(AuthResponse.RoleEnum.ADMIN)
                .hospitalId(admin.getHospitalId());
    }

    /**
     * Revokes the given JWT so it can no longer be used, even though it has not
     * yet naturally expired. Works for any authenticated role (patient, doctor, admin).
     */
    @Transactional
    public void logout(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("Authentication token is invalid or expired");
        }
        String jti = jwtUtil.extractJti(token);
        java.util.Date expiration = jwtUtil.extractExpiration(token);
        tokenBlacklistService.revoke(jti, expiration);
        log.info("Token revoked for userId={}, role={}", jwtUtil.extractUserId(token), jwtUtil.extractRole(token));
    }
}
