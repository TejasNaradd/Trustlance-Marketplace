package com.trustlance.contractservice.dto;

import com.trustlance.contractservice.model.MilestoneStatus;
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
public class MilestoneResponse {
    private UUID id;
    private UUID contractId;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private Integer deliveryDays;
    private MilestoneStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    // version is intentionally NOT exposed to clients
}
