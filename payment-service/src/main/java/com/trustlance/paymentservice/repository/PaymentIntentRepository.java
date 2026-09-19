package com.trustlance.paymentservice.repository;

import com.trustlance.paymentservice.model.PaymentIntent;
import com.trustlance.paymentservice.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);

    Page<PaymentIntent> findByBuyerId(UUID buyerId, Pageable pageable);

    Optional<PaymentIntent> findByMilestoneIdAndStatus(UUID milestoneId, PaymentStatus status);

    Optional<PaymentIntent> findByProviderOrderId(String providerOrderId);
}
