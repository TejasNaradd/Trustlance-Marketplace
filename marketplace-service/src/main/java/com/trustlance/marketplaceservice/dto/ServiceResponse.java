package com.trustlance.marketplaceservice.dto;

import com.trustlance.marketplaceservice.model.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private UUID serviceId;
    private UUID sellerId;
    private UUID categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String skills;
    private BigDecimal price;
    private Integer deliveryDays;
    private ServiceStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
