package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.entity.Patient;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.AdminUser;
import com.healthcare.entity.PasswordResetToken;
import com.healthcare.entity.SecurityQuestion;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AdminUserRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.repository.PasswordResetTokenRepository;
import com.healthcare.repository.SecurityQuestionRepository;
import com.healthcare.security.JwtUtil;
import com.healthcare.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

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
    private final SecurityQuestionRepository securityQuestionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetAttemptService passwordResetAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (patientRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS);
        }

        SecurityQuestion securityQuestion = securityQuestionRepository.findById(request.getSecurityQuestionId())
                .filter(question -> Boolean.TRUE.equals(question.getIsActive()))
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));

        Patient patient = Patient.builder()
                .name(request.getName())
                .gender(request.getGender())
                .dob(Date.valueOf(request.getDob()))
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .securityQuestionId(securityQuestion.getId())
                .securityAnswerHash(passwordEncoder.encode(normalizeAnswer(request.getSecurityAnswer())))
                .passwordVersion(0)
                .address(request.getAddress())
                .emergencyContactNumber(request.getEmergencyContactNumber())
                .bloodGroup(request.getBloodGroup() != null ? request.getBloodGroup().getValue() : null)
                .permanentIllness(request.getPermanentIllness())
                .createdAt(LocalDateTime.now())
                .build();

        patient = patientRepository.save(patient);
        log.info("Registered new patient with id={}", patient.getId());

        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null, patient.getPasswordVersion());
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

        if (!matchesPassword(request.getPassword(), patient.getPasswordHash())) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        log.info("Patient login successful for id={}", patient.getId());
        String token = jwtUtil.generateToken(patient.getId(), "PATIENT", null, patient.getPasswordVersion());
        return new AuthResponse()
                .token(token)
                .id(patient.getId())
                .name(patient.getName())
                .role(AuthResponse.RoleEnum.PATIENT);
    }

    private AuthResponse loginDoctor(LoginRequest request) {
        Doctor doctor = doctorRepository.findByPhone(request.getMobileNumber())
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS));

        if (!matchesPassword(request.getPassword(), doctor.getPasswordHash())) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_ACCOUNT_INACTIVE);
        }

        log.info("Doctor login successful for id={}", doctor.getId());
        String token = jwtUtil.generateToken(doctor.getId(), "DOCTOR", doctor.getHospitalId(), doctor.getPasswordVersion());
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

        if (!matchesPassword(request.getPassword(), admin.getPasswordHash())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new ResourceNotFoundException(MessageCode.AUTH_ACCOUNT_INACTIVE);
        }

        log.info("Admin login successful for id={}", admin.getId());
        String token = jwtUtil.generateToken(admin.getId(), "ADMIN", admin.getHospitalId(), admin.getPasswordVersion());
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
        log.info("Session revoked for userId={}, role={}", jwtUtil.extractUserId(token), jwtUtil.extractRole(token));
    }

    @Transactional(readOnly = true)
    public List<SecurityQuestion> getPasswordResetQuestions() {
        return securityQuestionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public PasswordResetResult verifyPasswordResetAnswer(String role, String identifier,
                                                         Long questionId, String answer) {
        String normalizedRole = normalizeRole(role);
        Account account = findResetAccount(normalizedRole, identifier);
        LocalDateTime now = LocalDateTime.now();
        if (passwordResetAttemptService.isLocked(normalizedRole, account.id(), now)) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        SecurityQuestion question = securityQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));

        boolean answerMatches = StringUtils.hasText(answer)
                && StringUtils.hasText(account.answerHash())
                && matchesPassword(normalizeAnswer(answer), account.answerHash());
        if (!Boolean.TRUE.equals(question.getIsActive())
                || account.questionId() == null
                || !questionId.equals(account.questionId())
                || !answerMatches) {
            passwordResetAttemptService.recordFailure(normalizedRole, account.id(), now);
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }

        passwordResetAttemptService.clear(normalizedRole, account.id());
        String rawToken = generateToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(hashToken(rawToken))
                .accountRole(normalizedRole)
                .accountId(account.id())
                .expiresAt(now.plusMinutes(10))
                .createdAt(now)
                .build();
        passwordResetTokenRepository.save(token);
        return new PasswordResetResult(rawToken,
                token.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
    }

    @Transactional
    public void completePasswordReset(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(hashToken(rawToken), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));

        String role = token.getAccountRole();
        if ("PATIENT".equals(role)) {
            Patient account = patientRepository.findById(token.getAccountId())
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            patientRepository.save(account);
        } else if ("DOCTOR".equals(role)) {
            Doctor account = doctorRepository.findById(token.getAccountId())
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            doctorRepository.save(account);
        } else if ("ADMIN".equals(role)) {
            AdminUser account = adminUserRepository.findById(token.getAccountId())
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            adminUserRepository.save(account);
        } else {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }

        token.setConsumedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    private Account findResetAccount(String role, String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        return switch (role) {
            case "PATIENT" -> patientRepository.findByMobileNumber(identifier)
                    .map(p -> new Account(p.getId(), p.getSecurityQuestionId(), p.getSecurityAnswerHash()))
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            case "DOCTOR" -> doctorRepository.findByPhone(identifier)
                    .map(d -> new Account(d.getId(), d.getSecurityQuestionId(), d.getSecurityAnswerHash()))
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            case "ADMIN" -> adminUserRepository.findByMobileNumber(identifier)
                    .map(a -> new Account(a.getId(), a.getSecurityQuestionId(), a.getSecurityAnswerHash()))
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            default -> throw new BadRequestException(MessageCode.AUTH_INVALID_ROLE);
        };
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) throw new BadRequestException(MessageCode.AUTH_INVALID_ROLE);
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PATIENT", "DOCTOR", "ADMIN").contains(normalized)) {
            throw new BadRequestException(MessageCode.AUTH_INVALID_ROLE);
        }
        return normalized;
    }

    private String normalizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Treat malformed legacy hashes as invalid credentials instead of allowing
     * BCryptPasswordEncoder to surface an IllegalArgumentException as HTTP 500.
     */
    private boolean matchesPassword(String rawValue, String encodedValue) {
        if (!StringUtils.hasText(rawValue) || !StringUtils.hasText(encodedValue)) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawValue, encodedValue);
        } catch (IllegalArgumentException exception) {
            log.warn("Ignoring malformed BCrypt credential hash during authentication");
            return false;
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password reset token", e);
        }
    }

    private int nextVersion(Integer version) {
        return version == null ? 1 : version + 1;
    }

    private record Account(Long id, Long questionId, String answerHash) { }

    public record PasswordResetResult(String resetToken, OffsetDateTime expiresAt) { }
}