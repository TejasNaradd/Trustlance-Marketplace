package com.trustlance.contractservice.event;

import com.trustlance.contractservice.model.Contract;
import com.trustlance.contractservice.model.Milestone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Centralized Kafka event publisher for contract-service.
 * All domain events go through here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CONTRACT_EVENTS_TOPIC = "contract-events";

    public void publishContractCreated(Contract contract) {
        publish(contract, null, "CONTRACT_CREATED");
    }

    public void publishMilestoneSubmitted(Contract contract, Milestone milestone) {
        publish(contract, milestone, "MILESTONE_SUBMITTED");
    }

    public void publishMilestoneApproved(Contract contract, Milestone milestone) {
        publish(contract, milestone, "MILESTONE_APPROVED");
    }

    public void publishContractCompleted(Contract contract) {
        publish(contract, null, "CONTRACT_COMPLETED");
    }

    private void publish(Contract contract, Milestone milestone, String eventType) {
        ContractEvent event = ContractEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .producer("contract-service")
                .occurredAt(Instant.now())
                .contractId(contract.getId())
                .milestoneId(milestone != null ? milestone.getId() : null)
                .buyerId(contract.getBuyerId())
                .sellerId(contract.getSellerId())
                .contractTitle(contract.getTitle())
                .milestoneTitle(milestone != null ? milestone.getTitle() : null)
                .build();

        kafkaTemplate.send(CONTRACT_EVENTS_TOPIC, contract.getId().toString(), event);
        log.info("Published {} for contract {}", eventType, contract.getId());
    }
}
