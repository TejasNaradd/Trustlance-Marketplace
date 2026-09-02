package com.trustlance.marketplaceservice.dto;

import com.trustlance.marketplaceservice.model.ServiceStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateServiceRequest {
    private UUID categoryId;

    @Size(min = 5, max = 150)
    private String title;

    @Size(min = 20)
    private String description;

    private String skills;

    @DecimalMin(value = "5.00")
    private BigDecimal price;

    @Min(value = 1)
    @Max(value = 90)
    private Integer deliveryDays;

    private ServiceStatus status;
}
