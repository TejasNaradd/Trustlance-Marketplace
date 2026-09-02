package com.trustlance.disputeservice.dto;

import com.trustlance.disputeservice.model.DisputeStatus;
import lombok.Data;

@Data
public class ResolveDisputeRequest {
    private DisputeStatus resolutionStatus;
    private String resolutionNotes;
}
