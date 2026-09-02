package com.trustlance.disputeservice.event;

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
public class DisputeEvent {
    private UUID eventId;
    private String eventType;   // DISPUTE_CREATED | DISPUTE_RESOLVED
    private String producer;
    private Instant occurredAt;
    private String correlationId;

    private String disputeId;
    private UUID contractId;
    private UUID milestoneId;
    private UUID initiatorId;
    private UUID respondentId;
    private String resolutionStatus; // only for DISPUTE_RESOLVED
    private String resolutionNotes;
}
