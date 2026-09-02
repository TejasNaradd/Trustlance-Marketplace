package com.trustlance.contractservice.dto;

import com.trustlance.contractservice.model.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {
    private UUID id;
    private UUID buyerId;
    private UUID sellerId;
    private UUID serviceId;
    private String title;
    private String description;
    private ContractStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    // version is intentionally NOT exposed to clients
}
