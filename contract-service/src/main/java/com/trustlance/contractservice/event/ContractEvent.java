package com.trustlance.contractservice.event;

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
public class ContractEvent {
    private UUID eventId;
    private String eventType;  // CONTRACT_CREATED | MILESTONE_SUBMITTED | MILESTONE_APPROVED | CONTRACT_COMPLETED
    private String producer;
    private Instant occurredAt;
    private String correlationId;

    private UUID contractId;
    private UUID milestoneId;
    private UUID buyerId;
    private UUID sellerId;
    private String contractTitle;
    private String milestoneTitle;
}
