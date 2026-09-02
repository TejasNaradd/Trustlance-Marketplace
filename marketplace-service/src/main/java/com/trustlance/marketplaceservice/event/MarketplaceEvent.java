package com.trustlance.marketplaceservice.event;

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
public class MarketplaceEvent {
    private UUID eventId;
    private String eventType;  // SERVICE_CREATED
    private String producer;
    private Instant occurredAt;

    private UUID serviceId;
    private UUID sellerId;
    private String title;
    private String categoryName;
}
