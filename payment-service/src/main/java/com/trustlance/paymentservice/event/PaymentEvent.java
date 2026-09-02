package com.trustlance.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event payload — published to "payment-events" topic.
 * Consumers must be idempotent using eventId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private UUID eventId;           // UUID for consumer idempotency
    private String eventType;       // PAYMENT_SUCCESS | PAYMENT_FAILED
    private String producer;        // "payment-service"
    private Instant occurredAt;
    private String correlationId;

    // Payload
    private UUID paymentIntentId;
    private UUID milestoneId;
    private UUID contractId;
    private UUID buyerId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerPaymentId;
    private String failureReason;   // populated on PAYMENT_FAILED
}
