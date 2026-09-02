package com.trustlance.disputeservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "disputes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    private String id;

    @Indexed
    private UUID contractId;

    @Indexed(unique = true) // one active dispute per milestone
    private UUID milestoneId;

    @Indexed
    private UUID initiatorId; // buyer or seller who opened the dispute

    private UUID respondentId;

    private String title;
    private String description;

    private DisputeStatus status;
    private UUID resolvedById; // ADMIN who resolved it
    private String resolutionNotes;

    @Builder.Default
    private List<Evidence> evidenceList = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Evidence {
        private String id;
        private UUID submitterId;
        private String notes;
        private String objectStorageRefs; // JSON array of file URLs
        private Instant submittedAt;
    }
}
