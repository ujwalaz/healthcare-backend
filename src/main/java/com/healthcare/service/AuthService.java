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
import com.healthcare.repository.AuthTransactionLock;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.OtpStateRepository;
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
import java.util.Objects;
import java.util.Optional;

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
    private final AuthTransactionLock authLock;
    private final OtpStateRepository otpState;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        String mobile = OtpService.normalizeMobile(request.getMobileNumber());
        authLock.acquire("otp:" + mobile);
        if (!otpState.consumeRegistration(mobile, hashToken(request.getVerificationToken()), LocalDateTime.now())) {
            throw new BadRequestException(MessageCode.AUTH_OTP_INVALID);
        }
        SecurityQuestion securityQuestion = securityQuestionRepository.findById(request.getSecurityQuestionId())
                .filter(question -> Boolean.TRUE.equals(question.getIsActive()))
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));

        Patient patient = patientRepository.findByMobileNumber(mobile).orElse(null);
        if (patient != null && Boolean.TRUE.equals(patient.getIsRegistered())) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS);
        }
        if (patient != null && Boolean.TRUE.equals(patient.getIsDeleted())) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS);
        }
        if (patient != null && (!patient.getName().strip().replaceAll("\\s+", " ")
                .equalsIgnoreCase(request.getName().strip().replaceAll("\\s+", " "))
                || (patient.getDob() != null && !patient.getDob().toLocalDate().equals(request.getDob()))
                || (patient.getGender() != null && !"OTHER".equals(patient.getGender())
                    && !patient.getGender().equalsIgnoreCase(request.getGender())))) {
            throw new ConflictException(MessageCode.PATIENT_MOBILE_EXISTS,
                    "Guest profile details require review. Contact support to complete registration.");
        }
        if (patient == null) {
            patient = Patient.builder()
                    .mobileNumber(mobile)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        patient.setName(request.getName());
        patient.setGender(request.getGender());
        patient.setDob(Date.valueOf(request.getDob()));
        patient.setMobileNumber(mobile);
        patient.setEmail(request.getEmail());
        patient.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        patient.setSecurityQuestionId(securityQuestion.getId());
        patient.setSecurityAnswerHash(passwordEncoder.encode(normalizeAnswer(request.getSecurityAnswer())));
        patient.setPasswordVersion(patient.getPasswordVersion() == null ? 0 : patient.getPasswordVersion());
        patient.setIsRegistered(true);
        patient.setAddress(request.getAddress());
        patient.setEmergencyContactNumber(request.getEmergencyContactNumber());
        patient.setBloodGroup(request.getBloodGroup() != null ? request.getBloodGroup().getValue() : null);
        patient.setPermanentIllness(request.getPermanentIllness());

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
        Patient patient = patientRepository.findByMobileNumberAndIsDeletedFalse(request.getMobileNumber())
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
        lockResetAccount(normalizedRole, account.id());
        LocalDateTime now = LocalDateTime.now();
        if (passwordResetAttemptService.isLocked(normalizedRole, account.id(), now)) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        boolean questionMatches = Objects.equals(questionId, account.questionId()) && questionId != null
                && securityQuestionRepository.findById(questionId)
                        .filter(question -> Boolean.TRUE.equals(question.getIsActive())).isPresent();
        boolean answerMatches = questionMatches && StringUtils.hasText(answer)
                && StringUtils.hasText(account.answerHash())
                && matchesPassword(normalizeAnswer(answer), account.answerHash());
        if (!answerMatches) {
            passwordResetAttemptService.recordFailure(normalizedRole, account.id(), now);
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }

        passwordResetAttemptService.clear(normalizedRole, account.id());
        return issueResetToken(normalizedRole, account.id(), now);
    }

    public Long findOtpResetAccountId(String role, String mobile) {
        return findEligibleResetAccount(role, mobile).map(Account::id).orElse(null);
    }

    @Transactional
    public PasswordResetResult issueOtpResetToken(String role, Long accountId, String mobile) {
        lockResetAccount(role, accountId);
        if (!accountId.equals(findOtpResetAccountId(role, mobile))) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        return issueResetToken(role, accountId, LocalDateTime.now());
    }

    private PasswordResetResult issueResetToken(String role, Long accountId, LocalDateTime now) {
        String rawToken = generateToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(hashToken(rawToken))
                .accountRole(role)
                .accountId(accountId)
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
        lockResetAccount(role, token.getAccountId());
        if (passwordResetTokenRepository.consume(token.getId(), LocalDateTime.now()) != 1) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        if ("PATIENT".equals(role)) {
            Patient account = patientRepository.lockActiveAccount(token.getAccountId())
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            patientRepository.save(account);
        } else if ("DOCTOR".equals(role)) {
            Doctor account = doctorRepository.findById(token.getAccountId())
                    .filter(doctor -> Boolean.TRUE.equals(doctor.getIsActive()))
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            doctorRepository.save(account);
        } else if ("ADMIN".equals(role)) {
            AdminUser account = adminUserRepository.findById(token.getAccountId())
                    .filter(admin -> Boolean.TRUE.equals(admin.getIsActive()))
                    .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setPasswordVersion(nextVersion(account.getPasswordVersion()));
            adminUserRepository.save(account);
        } else {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }

        passwordResetTokenRepository.revokeForAccount(role, token.getAccountId(), LocalDateTime.now());
    }

    private void lockResetAccount(String role, Long accountId) {
        authLock.acquire("reset:" + role + ":" + accountId);
    }

    private Account findResetAccount(String role, String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw new BadRequestException(MessageCode.AUTH_RESET_INVALID);
        }
        return findEligibleResetAccount(role, identifier)
                .orElseThrow(() -> new BadRequestException(MessageCode.AUTH_RESET_INVALID));
    }

    private Optional<Account> findEligibleResetAccount(String role, String identifier) {
        return switch (role) {
            case "PATIENT" -> patientRepository.findByMobileNumberAndIsDeletedFalse(identifier)
                    .filter(p -> Boolean.TRUE.equals(p.getIsRegistered()))
                    .map(p -> new Account(p.getId(), p.getSecurityQuestionId(), p.getSecurityAnswerHash()));
            case "DOCTOR" -> doctorRepository.findByPhone(identifier)
                    .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                    .map(d -> new Account(d.getId(), d.getSecurityQuestionId(), d.getSecurityAnswerHash()));
            case "ADMIN" -> adminUserRepository.findByMobileNumber(identifier)
                    .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                    .map(a -> new Account(a.getId(), a.getSecurityQuestionId(), a.getSecurityAnswerHash()));
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

    String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    String hashToken(String token) {
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