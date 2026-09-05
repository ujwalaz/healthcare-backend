package com.healthcare.controller;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/patient/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerPatient(
            @Valid @RequestBody PatientRegisterRequest request) {
        AuthResponse response = authService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageCode.AUTH_REGISTER_SUCCESS, response));
    }

    @PostMapping("/patient/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginPatient(
            @Valid @RequestBody PatientLoginRequest request) {
        AuthResponse response = authService.loginPatient(request);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.AUTH_LOGIN_SUCCESS, response));
    }

    @PostMapping("/doctor/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginDoctor(
            @Valid @RequestBody DoctorLoginRequest request) {
        AuthResponse response = authService.loginDoctor(request);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.AUTH_LOGIN_SUCCESS, response));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(
            @Valid @RequestBody AdminLoginRequest request) {
        AuthResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.AUTH_LOGIN_SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.AUTH_LOGOUT_SUCCESS, null));
    }
}