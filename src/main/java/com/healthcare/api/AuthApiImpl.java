package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.AuthResponse;
import com.healthcare.dto.LoginRequest;
import com.healthcare.dto.PatientRegisterRequest;
import com.healthcare.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final AuthService authService;

    @Override
    @ApiMessage(MessageCode.AUTH_REGISTER_SUCCESS)
    public ResponseEntity<AuthResponse> registerPatient(PatientRegisterRequest patientRegisterRequest) {
        AuthResponse response = authService.registerPatient(patientRegisterRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @ApiMessage(MessageCode.AUTH_LOGIN_SUCCESS)
    public ResponseEntity<AuthResponse> login(String role, LoginRequest loginRequest) {
        AuthResponse response = authService.login(role, loginRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")
                || authorization.length() <= 7 || !StringUtils.hasText(authorization.substring(7))) {
            throw new BadCredentialsException("Authorization header with a Bearer token is required");
        }

        String token = authorization.substring(7).trim();
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
