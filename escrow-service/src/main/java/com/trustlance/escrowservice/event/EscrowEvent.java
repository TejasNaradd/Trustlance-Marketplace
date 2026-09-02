package com.trustlance.escrowservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event published by escrow-service to "escrow-events" topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowEvent {
    private UUID eventId;
    private String eventType;   // ESCROW_FUNDED | ESCROW_RELEASED | ESCROW_REFUNDED | ESCROW_FROZEN
    private String producer;
    private Instant occurredAt;
    private String correlationId;

    private UUID escrowId;
    private UUID milestoneId;
    private UUID contractId;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal amount;
    private String currency;
}
