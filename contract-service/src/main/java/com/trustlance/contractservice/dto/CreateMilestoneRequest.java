package com.trustlance.contractservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMilestoneRequest {

    @NotBlank(message = "Milestone title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Milestone amount must be at least 1.00")
    private BigDecimal amount;

    @Size(max = 10)
    private String currency = "USD";

    @NotNull(message = "Delivery days is required")
    @Min(value = 1, message = "Delivery must be at least 1 day")
    @Max(value = 365, message = "Delivery cannot exceed 365 days")
    private Integer deliveryDays;
}
