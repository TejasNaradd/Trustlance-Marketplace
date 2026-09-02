package com.trustlance.disputeservice.dto;

import com.trustlance.disputeservice.model.Dispute;
import com.trustlance.disputeservice.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {
    private String id;
    private UUID contractId;
    private UUID milestoneId;
    private UUID initiatorId;
    private UUID respondentId;
    private String title;
    private String description;
    private DisputeStatus status;
    private UUID resolvedById;
    private String resolutionNotes;
    private List<Dispute.Evidence> evidenceList;
    private Instant createdAt;
    private Instant updatedAt;
}
