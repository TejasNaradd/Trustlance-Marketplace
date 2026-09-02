package com.trustlance.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists raw webhook payloads from payment providers for audit and idempotent processing.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_provider_event", columnList = "provider, provider_event_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    // Provider-assigned unique event ID — used for idempotent processing
    @Column(name = "provider_event_id", nullable = false, length = 255)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false, nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
