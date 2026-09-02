package com.trustlance.escrowservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID eventId;
    private String eventType; // PAYMENT_SUCCESS | PAYMENT_FAILED
    private String producer;
    private Instant occurredAt;
    private String correlationId;
    
    private UUID paymentIntentId;
    private UUID milestoneId;
    private UUID contractId;
    private UUID buyerId;
    private UUID sellerId; // Add this if needed, or escrow will need to know it via some other way. Actually in payment-service PaymentEvent, we didn't include sellerId.
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerPaymentId;
    private String failureReason;
}
