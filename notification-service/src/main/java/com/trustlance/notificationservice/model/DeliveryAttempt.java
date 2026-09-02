package com.trustlance.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tracks each delivery attempt (in-app, email, etc.) for a notification.
 */
@Document(collection = "delivery_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAttempt {

    @Id
    private String id;

    @Indexed
    private String notificationId;

    private String channel;  // IN_APP | EMAIL
    private boolean success;
    private String errorMessage;
    private Instant attemptedAt;
}
