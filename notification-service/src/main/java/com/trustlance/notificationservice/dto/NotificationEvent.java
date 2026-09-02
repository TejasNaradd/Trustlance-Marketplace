package com.trustlance.notificationservice.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class NotificationEvent {
    private UUID recipientId;
    private String type; // matches NotificationType
    private String title;
    private String message;
    private String referenceId;
    private String referenceType;
    private Instant timestamp;
}
