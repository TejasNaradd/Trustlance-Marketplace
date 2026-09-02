package com.trustlance.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReviewRequest {
    
    @NotNull(message = "Contract ID is required")
    private UUID contractId;
    
    @NotNull(message = "Target user ID is required")
    private UUID targetId;
    
    @NotBlank(message = "Target role is required")
    private String targetRole;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    
    private String comment;
}
