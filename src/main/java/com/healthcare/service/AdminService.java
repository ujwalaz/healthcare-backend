package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.AdminProfileResponse;
import com.healthcare.dto.AdminProfileUpdateRequest;
import com.healthcare.entity.AdminUser;
import com.healthcare.exception.ConflictException;
import com.healthcare.exception.BadRequestException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminUserRepository adminUserRepository;

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(Long adminId) {
        return toResponse(findAdmin(adminId));
    }

    @Transactional
    public AdminProfileResponse updateProfile(Long adminId, AdminProfileUpdateRequest request) {
        AdminUser admin = findAdmin(adminId);

        if (request.getName() == null && request.getEmail() == null) {
            throw new BadRequestException(MessageCode.VALIDATION_FAILED, "At least one profile field is required");
        }

        if (request.getName() != null) {
            admin.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(admin.getEmail())) {
            if (adminUserRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(adminId)).isPresent()) {
                throw new ConflictException(MessageCode.ADMIN_EMAIL_EXISTS);
            }
            admin.setEmail(request.getEmail());
        }

        adminUserRepository.save(admin);
        log.info("Updated profile for adminId={}", adminId);
        return toResponse(admin);
    }

    private AdminUser findAdmin(Long adminId) {
        return adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.ADMIN_NOT_FOUND));
    }

    private AdminProfileResponse toResponse(AdminUser admin) {
        return new AdminProfileResponse()
                .id(admin.getId())
                .name(admin.getName())
                .mobileNumber(admin.getMobileNumber())
                .email(admin.getEmail())
                .hospitalId(admin.getHospitalId())
                .role(admin.getRole())
                .isActive(admin.getIsActive())
                .createdAt(toOffsetDateTime(admin.getCreatedAt()));
    }

    private java.time.OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}