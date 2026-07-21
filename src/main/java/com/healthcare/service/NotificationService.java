package com.healthcare.service;

import com.healthcare.dto.*;
import com.healthcare.constants.MessageCode;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Boolean isRead, Pageable pageable, JwtClaims caller) {
        Page<Notification> page = notificationRepository.findByRecipientWithReadFilter(
                caller.role(), caller.userId(), isRead, pageable);

        Page<NotificationResponse> responsePage = page.map(n -> new NotificationResponse()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(toOffsetDateTime(n.getCreatedAt())));

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

        return new MarkReadResponse()
                .id(notification.getId())
                .isRead(notification.getIsRead());
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

        return new NotificationSentResponse()
                .id(notification.getId())
                .recipientUserType(notification.getRecipientUserType())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .isRead(notification.getIsRead())
                .createdAt(toOffsetDateTime(notification.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(JwtClaims caller) {
        long count = notificationRepository.countByRecipientUserTypeAndRecipientIdAndIsRead(
                caller.role(), caller.userId(), false);
        log.info("Unread notification count={} for userId={}", count, caller.userId());
        return new UnreadCountResponse().unreadCount((int) count);
    }

    @Transactional
    public void markAllAsRead(JwtClaims caller) {
        notificationRepository.markAllAsReadByRecipient(caller.role(), caller.userId());
        log.info("All notifications marked as read for userId={}", caller.userId());
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}