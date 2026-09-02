package com.trustlance.paymentservice.dto;

import com.trustlance.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private UUID id;
    private UUID milestoneId;
    private UUID contractId;
    private UUID buyerId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerOrderId;
    private PaymentStatus status;
    private Map<String, String> clientMetadata; // provider-specific data for frontend
    private Instant createdAt;
    private Instant updatedAt;
}
