package com.trustlance.disputeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitEvidenceRequest {
    private String notes;
    
    @NotBlank(message = "Object storage references are required")
    private String objectStorageRefs;
}
