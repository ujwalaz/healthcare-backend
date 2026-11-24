package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.exception.BadRequestException;
import com.healthcare.repository.AuthTransactionLock;
import com.healthcare.repository.OtpStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@Slf4j
public class OtpService {
    public enum Purpose { REGISTRATION, PASSWORD_RESET }
    public enum Role { PATIENT, DOCTOR, ADMIN }

    public record SendResult(int expiresInSeconds, int resendAfterSeconds) {}
    public record VerifyResult(String verificationToken, String resetToken, OffsetDateTime expiresAt) {}

    private final Fast2SmsOtpClient provider;
    private final OtpStateRepository state;
    private final AuthTransactionLock lock;
    private final OtpRateLimiter limiter;
    private final AuthService auth;
    private final int expiryMinutes;
    private final int resendSeconds;

    public OtpService(Fast2SmsOtpClient provider, OtpStateRepository state, AuthTransactionLock lock,
                      OtpRateLimiter limiter, AuthService auth,
                      @Value("${app.otp.expiry-minutes:5}") int expiryMinutes,
                      @Value("${app.otp.resend-seconds:60}") int resendSeconds) {
        if (expiryMinutes < 1 || expiryMinutes > 15 || resendSeconds < 1 || resendSeconds >= expiryMinutes * 60) {
            throw new IllegalArgumentException("Invalid OTP expiry or resend cooldown");
        }
        this.provider = provider;
        this.state = state;
        this.lock = lock;
        this.limiter = limiter;
        this.auth = auth;
        this.expiryMinutes = expiryMinutes;
        this.resendSeconds = resendSeconds;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public SendResult send(String mobileNumber, Purpose purpose, Role role, String ip) {
        String mobile = normalizeMobile(mobileNumber);
        String roleName = validateRole(purpose, role);
        provider.requireConfigured();
        limiter.check("send", mobile, ip, 5, 30);
        lock.acquire("otp:" + mobile);
        LocalDateTime now = LocalDateTime.now();
        Long accountId = purpose == Purpose.PASSWORD_RESET ? auth.findOtpResetAccountId(roleName, mobile) : null;
        // Fast2SMS verifies by mobile number. Keep the target fixed until a previous
        // provider code expires, including when the outcome of its send was unknown.
        if (state.find(mobile).filter(record -> record.resendAfter().isAfter(now)
                || (record.expiresAt().isAfter(now) && (!record.matches(purpose.name(), roleName)
                    || !Objects.equals(record.accountId(), accountId)))).isPresent()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another code");
        }

        state.replace(mobile, purpose.name(), roleName, accountId,
                now.plusMinutes(expiryMinutes), now.plusSeconds(resendSeconds));
        if (purpose == Purpose.REGISTRATION || accountId != null) {
            try {
                if (!provider.send(mobile, expiryMinutes)) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS verification is temporarily unavailable");
                }
                state.markSent(mobile);
            } catch (ResponseStatusException e) {
                if (purpose == Purpose.REGISTRATION) throw e;
                log.warn("OTP provider did not accept a password reset SMS");
            }
        }
        return new SendResult(expiryMinutes * 60, resendSeconds);
    }

    @Transactional(noRollbackFor = {BadRequestException.class, ResponseStatusException.class})
    public VerifyResult verify(String mobileNumber, Purpose purpose, Role role, String otp, String ip) {
        String mobile = normalizeMobile(mobileNumber);
        String roleName = validateRole(purpose, role);
        provider.requireConfigured();
        limiter.check("verify", mobile, ip, 15, 60);
        lock.acquire("otp:" + mobile);
        LocalDateTime now = LocalDateTime.now();
        OtpStateRepository.State record = state.find(mobile)
                .filter(value -> value.pending() && value.matches(purpose.name(), roleName)
                        && value.expiresAt().isAfter(now) && value.attempts() < 5)
                .orElseThrow(OtpService::invalid);
        if (purpose == Purpose.PASSWORD_RESET
                && !Objects.equals(record.accountId(), auth.findOtpResetAccountId(roleName, mobile))) {
            throw invalid();
        }
        state.recordAttempt(mobile);
        if (!provider.verify(mobile, otp)) throw invalid();

        if (purpose == Purpose.PASSWORD_RESET) {
            AuthService.PasswordResetResult result = auth.issueOtpResetToken(roleName, record.accountId(), mobile);
            state.endPasswordResetOtp(mobile);
            return new VerifyResult(null, result.resetToken(), result.expiresAt());
        }
        String token = auth.generateToken();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        state.markVerified(mobile, auth.hashToken(token), expiry);
        return new VerifyResult(token, null, expiry.atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    static String normalizeMobile(String number) {
        String mobile = number == null ? "" : number.strip();
        if (mobile.matches("\\+91[0-9]{10}")) mobile = mobile.substring(3);
        if (!mobile.matches("[0-9]{10}")) {
            throw new BadRequestException(MessageCode.VALIDATION_FAILED, "Enter a 10-digit Indian mobile number");
        }
        return mobile;
    }

    private static String validateRole(Purpose purpose, Role role) {
        if (purpose == Purpose.PASSWORD_RESET && role == null || purpose == Purpose.REGISTRATION && role != null) {
            throw new BadRequestException(MessageCode.VALIDATION_FAILED,
                    "Role is required for password reset and must be omitted for registration");
        }
        return role == null ? null : role.name();
    }

    private static BadRequestException invalid() {
        return new BadRequestException(MessageCode.AUTH_OTP_INVALID);
    }
}