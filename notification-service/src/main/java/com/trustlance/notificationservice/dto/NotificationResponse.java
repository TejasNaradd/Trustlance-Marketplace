package com.trustlance.notificationservice.dto;

import com.trustlance.notificationservice.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private UUID recipientId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
    private String referenceType;
    private boolean read;
    private Instant createdAt;
}
