package com.trustlance.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * PaymentIntent: tracks the lifecycle of a buyer's attempt to fund a milestone.
 * The frontend never declares success; only a verified webhook can mark SUCCESS.
 */
@Entity
@Table(name = "payment_intents", indexes = {
    @Index(name = "idx_pi_milestone", columnList = "milestone_id"),
    @Index(name = "idx_pi_buyer", columnList = "buyer_id"),
    @Index(name = "idx_pi_idempotency", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_pi_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "milestone_id", nullable = false)
    private UUID milestoneId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // e.g. "razorpay", "stripe"

    // Provider-assigned order/session ID
    @Column(name = "provider_order_id", length = 255)
    private String providerOrderId;

    // Provider-assigned payment ID (set after payment captured)
    @Column(name = "provider_payment_id", length = 255)
    private String providerPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    // Idempotency key prevents duplicate intents for the same milestone
    @Column(name = "idempotency_key", nullable = false, length = 255, unique = true)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
