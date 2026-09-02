package com.trustlance.userservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    private static final String USER_EVENTS_TOPIC = "user-events";

    public void publishUserRegistered(UUID userId, String email, String role) {
        UserEvent event = UserEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("USER_REGISTERED")
                .producer("user-service")
                .occurredAt(Instant.now())
                .userId(userId)
                .email(email)
                .role(role)
                .build();

        kafkaTemplate.send(USER_EVENTS_TOPIC, userId.toString(), event);
        log.info("Published USER_REGISTERED for user {}", userId);
    }
}
