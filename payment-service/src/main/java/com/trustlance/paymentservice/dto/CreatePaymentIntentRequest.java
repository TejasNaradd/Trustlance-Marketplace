package com.trustlance.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePaymentIntentRequest {

    @NotNull(message = "Milestone ID is required")
    private UUID milestoneId;

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    private BigDecimal amount;

    private String currency = "USD";

    @NotBlank(message = "Provider is required")
    private String provider; // "razorpay" | "stripe" | "mock"

    // Client-supplied idempotency key — prevents duplicate intents
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
