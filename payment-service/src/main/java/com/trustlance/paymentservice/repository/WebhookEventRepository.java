package com.trustlance.paymentservice.repository;

import com.trustlance.paymentservice.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    // Used for idempotent webhook processing — if exists, skip processing
    Optional<WebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
