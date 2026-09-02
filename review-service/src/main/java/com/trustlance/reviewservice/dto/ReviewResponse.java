package com.trustlance.reviewservice.dto;

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
public class ReviewResponse {
    private String id;
    private UUID contractId;
    private UUID reviewerId;
    private UUID targetId;
    private String targetRole;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
