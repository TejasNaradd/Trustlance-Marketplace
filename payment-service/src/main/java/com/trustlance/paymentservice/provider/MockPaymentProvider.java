package com.trustlance.paymentservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stub/mock provider for development and testing.
 * Replace with real Razorpay/Stripe SDK implementation in production.
 */
@Component
@Slf4j
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public ProviderOrderResult createOrder(com.trustlance.paymentservice.model.PaymentIntent intent) {
        // Simulate provider order creation
        String mockOrderId = "mock_order_" + intent.getId();
        log.info("[MockProvider] Created order {} for intent {}", mockOrderId, intent.getId());
        return new ProviderOrderResult(mockOrderId, Map.of(
                "key", "mock_key",
                "amount", intent.getAmount().toPlainString(),
                "currency", intent.getCurrency()
        ));
    }

    @Override
    public void verifyWebhookSignature(String rawPayload, String signature, String secret) {
        // In production: compute HMAC-SHA256 of rawPayload with secret and compare to signature
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing webhook signature");
        }
        log.debug("[MockProvider] Webhook signature verified (mock)");
    }

    @Override
    public WebhookEventData extractEventData(String rawPayload) {
        // In production: parse JSON payload from provider
        return new WebhookEventData(
                "mock_event_" + System.currentTimeMillis(),
                "payment.captured",
                "mock_payment_id",
                true
        );
    }
}
