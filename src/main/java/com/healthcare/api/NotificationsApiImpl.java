package com.healthcare.api;

import com.healthcare.annotation.ApiMessage;
import com.healthcare.constants.MessageCode;
import com.healthcare.dto.MarkReadResponse;
import com.healthcare.dto.NotificationPagedResponse;
import com.healthcare.dto.NotificationRequest;
import com.healthcare.dto.NotificationResponse;
import com.healthcare.dto.NotificationSentResponse;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.UnreadCountResponse;
import com.healthcare.security.JwtClaims;
import com.healthcare.security.SecurityUtils;
import com.healthcare.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationsApiImpl implements NotificationsApi {

    private final NotificationService notificationService;

    @Override
    @ApiMessage(MessageCode.NOTIFICATION_FETCHED)
    public ResponseEntity<NotificationPagedResponse> getMyNotifications(Boolean isRead, Integer page, Integer size) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        Pageable pageable = PaginationUtils.of(page, size);
        PagedResponse<NotificationResponse> paged = notificationService.getMyNotifications(isRead, pageable, caller);
        NotificationPagedResponse response = new NotificationPagedResponse()
                .content(paged.getContent())
                .page(paged.getPage())
                .size(paged.getSize())
                .totalElements(paged.getTotalElements())
                .totalPages(paged.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.NOTIFICATION_MARKED_READ)
    public ResponseEntity<MarkReadResponse> markNotificationRead(Long id) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        MarkReadResponse response = notificationService.markAsRead(id, caller);
        return ResponseEntity.ok(response);
    }

    @Override
    @ApiMessage(MessageCode.NOTIFICATION_SENT)
    public ResponseEntity<NotificationSentResponse> sendNotification(NotificationRequest notificationRequest) {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        NotificationSentResponse response = notificationService.createNotification(notificationRequest, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @ApiMessage(MessageCode.NOTIFICATION_COUNT_FETCHED)
    public ResponseEntity<UnreadCountResponse> getUnreadNotificationCount() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        UnreadCountResponse response = notificationService.getUnreadCount(caller);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsRead() {
        JwtClaims caller = SecurityUtils.getCurrentClaims();
        notificationService.markAllAsRead(caller);
        return ResponseEntity.ok().build();
    }
}
