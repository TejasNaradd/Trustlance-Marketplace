package com.trustlance.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private UUID recipientId;

    private NotificationType type;

    private String title;
    private String message;

    private String referenceId; // e.g. contractId, disputeId
    private String referenceType; // e.g. "CONTRACT", "DISPUTE"

    private boolean read;

    @CreatedDate
    private Instant createdAt;
}
