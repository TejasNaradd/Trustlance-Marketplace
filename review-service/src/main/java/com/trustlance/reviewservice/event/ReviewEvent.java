package com.trustlance.reviewservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {
    private UUID eventId;
    private String eventType;   // REVIEW_CREATED
    private String producer;
    private Instant occurredAt;

    private String reviewId;
    private UUID contractId;
    private UUID reviewerId;
    private UUID targetId;
    private Integer rating;
}
