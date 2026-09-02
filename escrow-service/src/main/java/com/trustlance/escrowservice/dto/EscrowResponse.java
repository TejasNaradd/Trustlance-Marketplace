package com.trustlance.escrowservice.dto;

import com.trustlance.escrowservice.model.EscrowStatus;
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
public class EscrowResponse {
    private UUID id;
    private UUID milestoneId;
    private UUID contractId;
    private UUID buyerId;
    private UUID sellerId;
    private BigDecimal amount;
    private String currency;
    private EscrowStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
