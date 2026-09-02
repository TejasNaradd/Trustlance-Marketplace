package com.trustlance.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Processed event record for consumer idempotency.
 * If an eventId exists here, the event has already been processed.
 */
@Document(collection = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private String eventType;
    private Instant processedAt;
}
