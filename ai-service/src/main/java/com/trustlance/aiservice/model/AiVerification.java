package com.trustlance.aiservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "ai_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiVerification {

    @Id
    private String id;

    private UUID userId;
    private String documentType; // ID_CARD, PASSPORT, INVOICE
    private String objectStorageRef; // file URL

    private VerificationStatus status;
    private Double confidenceScore;
    private String extractedData; // JSON or text

    @CreatedDate
    private Instant createdAt;
    
    public enum VerificationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REQUIRES_MANUAL_REVIEW
    }
}
