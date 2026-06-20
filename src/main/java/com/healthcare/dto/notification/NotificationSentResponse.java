package com.healthcare.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSentResponse {

    private Long id;
    private String recipientUserType;
    private Long recipientId;
    private String title;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
