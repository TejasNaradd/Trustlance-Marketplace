package com.trustlance.contractservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submission_milestone", columnList = "milestone_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "milestone_id", nullable = false)
    private UUID milestoneId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Object storage references stored as JSON string (binary files are NOT in MySQL)
    @Column(name = "object_storage_refs", columnDefinition = "TEXT")
    private String objectStorageRefs;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;
}
