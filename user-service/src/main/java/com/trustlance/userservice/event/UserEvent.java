package com.trustlance.userservice.event;

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
public class UserEvent {
    private UUID eventId;
    private String eventType;  // USER_REGISTERED
    private String producer;
    private Instant occurredAt;

    private UUID userId;
    private String email;
    private String role;
}
