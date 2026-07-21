package com.healthcare.controller;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<NotificationResponse> response =
                notificationService.getMyNotifications(isRead, pageable, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.NOTIFICATION_FETCHED, response));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<MarkReadResponse>> markAsRead(@PathVariable Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        MarkReadResponse response = notificationService.markAsRead(id, caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.NOTIFICATION_MARKED_READ, response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationSentResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        NotificationSentResponse response = notificationService.createNotification(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageCode.NOTIFICATION_SENT, response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        UnreadCountResponse response = notificationService.getUnreadCount(caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.NOTIFICATION_COUNT_FETCHED, response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        notificationService.markAllAsRead(caller);
        return ResponseEntity.ok(ApiResponse.ok(MessageCode.NOTIFICATION_ALL_MARKED_READ, null));
    }
}