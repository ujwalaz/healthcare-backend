package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.AdminProfileResponse;
import com.healthcare.dto.AdminProfileUpdateRequest;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminApiImpl implements AdminApi {

    private final AdminService adminService;

    @Override
    @ApiMessage(MessageCode.ADMIN_PROFILE_FETCHED)
    public ResponseEntity<AdminProfileResponse> getAdminProfile() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("ADMIN");
        return ResponseEntity.ok(adminService.getProfile(caller.userId()));
    }

    @Override
    @ApiMessage(MessageCode.ADMIN_PROFILE_UPDATED)
    public ResponseEntity<AdminProfileResponse> updateAdminProfile(AdminProfileUpdateRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        SecurityUtils.requireRole("ADMIN");
        return ResponseEntity.ok(adminService.updateProfile(caller.userId(), request));
    }
}