package com.trustlance.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificationRequest {

    @NotBlank(message = "Document type is required")
    private String documentType;
    
    @NotBlank(message = "Object storage reference is required")
    private String objectStorageRef;
}
