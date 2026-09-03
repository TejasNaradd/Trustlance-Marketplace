package com.trustlance.aiservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published to "ai-events" topic when document verification completes.
 * Consumed by user-service to mark users as verified and notification-service for alerts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVerificationEvent {
    private UUID eventId;
    private String eventType;       // VERIFICATION_COMPLETED
    private String producer;
    private Instant occurredAt;

    private String verificationId;
    private UUID userId;
    private String documentType;
    private String status;          // APPROVED | REJECTED | REQUIRES_MANUAL_REVIEW
    private Double confidenceScore;
}
