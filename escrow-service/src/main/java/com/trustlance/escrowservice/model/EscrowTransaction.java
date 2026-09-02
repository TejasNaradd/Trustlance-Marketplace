package com.trustlance.escrowservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, append-only escrow transaction history.
 * Every state change on an Escrow record creates one of these for auditability.
 */
@Entity
@Table(name = "escrow_transactions", indexes = {
    @Index(name = "idx_etx_escrow", columnList = "escrow_id"),
    @Index(name = "idx_etx_idempotency", columnList = "idempotency_key", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "escrow_id", nullable = false)
    private UUID escrowId;

    @Column(name = "milestone_id", nullable = false)
    private UUID milestoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private EscrowStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private EscrowStatus toStatus;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
