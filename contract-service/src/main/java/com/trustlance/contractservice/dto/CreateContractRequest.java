package com.trustlance.contractservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateContractRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    // The seller this contract is being offered to
    // jakarta.validation.constraints.NotNull used because UUID is not a String
    private UUID sellerId;

    // Optional: reference to the marketplace service listing
    private UUID serviceId;
}
