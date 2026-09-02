package com.trustlance.paymentservice.model;

public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    RETRYABLE,
    FINAL_FAILED,
    REFUNDED
}
