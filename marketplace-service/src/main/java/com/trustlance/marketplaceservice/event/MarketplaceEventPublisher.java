package com.trustlance.marketplaceservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketplaceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String MARKETPLACE_EVENTS_TOPIC = "marketplace-events";

    public void publishServiceCreated(UUID serviceId, UUID sellerId, String title, String categoryName) {
        MarketplaceEvent event = MarketplaceEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("SERVICE_CREATED")
                .producer("marketplace-service")
                .occurredAt(Instant.now())
                .serviceId(serviceId)
                .sellerId(sellerId)
                .title(title)
                .categoryName(categoryName)
                .build();

        kafkaTemplate.send(MARKETPLACE_EVENTS_TOPIC, serviceId.toString(), event);
        log.info("Published SERVICE_CREATED for service {}", serviceId);
    }
}
