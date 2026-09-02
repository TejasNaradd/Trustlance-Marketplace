package com.trustlance.marketplaceservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateServiceRequest {
    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Description must be at least 20 characters")
    private String description;

    private String skills;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "5.00", message = "Minimum price is $5.00")
    private BigDecimal price;

    @NotNull(message = "Delivery days is required")
    @Min(value = 1, message = "Delivery time must be at least 1 day")
    @Max(value = 90, message = "Delivery time cannot exceed 90 days")
    private Integer deliveryDays;
}
