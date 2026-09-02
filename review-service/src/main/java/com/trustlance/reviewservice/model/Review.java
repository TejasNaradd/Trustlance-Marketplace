package com.trustlance.reviewservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "reviews")
@CompoundIndex(name = "reviewer_target_contract_idx", def = "{'reviewerId': 1, 'targetId': 1, 'contractId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;

    private UUID contractId;
    private UUID reviewerId;
    private UUID targetId;
    private String targetRole; // e.g. BUYER, SELLER
    
    private Integer rating;    // 1-5 scale
    private String comment;
    
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
