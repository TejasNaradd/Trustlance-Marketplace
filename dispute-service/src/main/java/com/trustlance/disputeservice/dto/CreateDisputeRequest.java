package com.trustlance.disputeservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDisputeRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    @NotNull(message = "Milestone ID is required")
    private UUID milestoneId;

    @NotNull(message = "Respondent ID is required")
    private UUID respondentId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;
}
