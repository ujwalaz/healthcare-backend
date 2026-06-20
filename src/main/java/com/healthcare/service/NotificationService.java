package com.healthcare.service;

import com.healthcare.constants.MessageCode;
import com.healthcare.dto.PagedResponse;
import com.healthcare.dto.notification.MarkReadResponse;
import com.healthcare.dto.notification.NotificationRequest;
import com.healthcare.dto.notification.NotificationResponse;
import com.healthcare.dto.notification.NotificationSentResponse;
import com.healthcare.entity.Notification;
import com.healthcare.exception.AppDeniedException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.NotificationRepository;
import com.healthcare.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Boolean isRead, Pageable pageable, JwtClaims caller) {
        Page<Notification> page = notificationRepository.findByRecipientWithReadFilter(
                caller.role(), caller.userId(), isRead, pageable);

        Page<NotificationResponse> responsePage = page.map(n -> NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build());

        return PagedResponse.from(responsePage);
    }

    @Transactional
    public MarkReadResponse markAsRead(Long notificationId, JwtClaims caller) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getRecipientUserType().equals(caller.role()) ||
            !notification.getRecipientId().equals(caller.userId())) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "You can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification id={} marked as read", notificationId);

        return MarkReadResponse.builder()
                .id(notification.getId())
                .isRead(notification.getIsRead())
                .build();
    }

    @Transactional
    public NotificationSentResponse createNotification(NotificationRequest request, JwtClaims caller) {
        if (!"ADMIN".equals(caller.role())) {
            throw new AppDeniedException(MessageCode.AUTH_UNAUTHORIZED,
                    "Only ADMIN can send notifications");
        }

        Notification notification = Notification.builder()
                .recipientUserType(request.getRecipientUserType().toUpperCase())
                .recipientId(request.getRecipientId())
                .title(request.getTitle())
                .message(request.getMessage())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created with id={} for recipientId={}", notification.getId(), request.getRecipientId());

        return NotificationSentResponse.builder()
                .id(notification.getId())
                .recipientUserType(notification.getRecipientUserType())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
